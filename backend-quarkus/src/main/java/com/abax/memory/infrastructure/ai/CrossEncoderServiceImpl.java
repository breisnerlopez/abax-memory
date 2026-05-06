package com.abax.memory.infrastructure.ai;

import com.abax.memory.domain.model.RerankedHit;
import com.abax.memory.domain.service.CrossEncoderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenAI-backed implementation of {@link CrossEncoderService} — v2.1.0.
 *
 * <p>Uses the same {@link ChatLanguageModel} CDI bean as
 * {@link OpenAiLlmService} (gpt-4o-mini) to evaluate entailment for
 * each (query, document) pair in a single batch prompt. A configurable
 * timeout (default 5s, was 2s in v2.1.0) guards the pipeline; on timeout
 * or failure the service degrades gracefully by returning empty results,
 * which signals {@link com.abax.memory.infrastructure.service.SearchServiceImpl}
 * to fall back to dense-only ordering.</p>
 *
 * <h3>Graceful Degradation</h3>
 * <ul>
 *   <li><b>Unavailable</b>: if {@code ChatLanguageModel} is not resolvable,
 *       returns empty list — caller uses dense-only.</li>
 *   <li><b>Timeout > configured</b>: cancels, logs {@code WARN CROSS_ENCODER_TIMEOUT},
 *       returns empty list.</li>
 *   <li><b>Malformed response</b>: logs {@code ERROR}, omits problematic
 *       candidates, returns best-effort partial results.</li>
 * </ul>
 *
 * <p>References: ADR-001, FT-V21-001.1</p>
 */
public class CrossEncoderServiceImpl implements CrossEncoderService {

    private static final Logger LOG = Logger.getLogger(CrossEncoderServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_BATCH_SIZE = 20;

    private final ChatLanguageModel chatModel;
    private final Duration timeout;
    private final ExecutorService executor;

    public CrossEncoderServiceImpl(ChatLanguageModel chatModel, Duration timeout) {
        this.chatModel = chatModel;
        this.timeout = timeout;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "cross-encoder-worker");
            t.setDaemon(true);
            return t;
        });
        LOG.infov("CrossEncoderServiceImpl initialized: chatModel={0}, timeout={1}s",
                chatModel, timeout.toSeconds());
    }

    @Override
    public List<RerankedHit> rerank(String query, List<CandidateDocument> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // Limit to MAX_BATCH_SIZE to stay within token limits and latency budget
        List<CandidateDocument> batch = candidates.size() > MAX_BATCH_SIZE
                ? candidates.subList(0, MAX_BATCH_SIZE)
                : candidates;

        String prompt = buildCrossEncoderPrompt(query, batch);

        try {
            String response = executeWithTimeout(prompt);
            Map<String, Double> crossScores = parseCrossEncoderResponse(response);

            // Build reranked hits, combining semantic + cross-encoder scores
            // If no candidates received a cross-encoder score, treat as failure
            // and return empty to signal graceful degradation to the caller.
            if (crossScores.isEmpty()) {
                LOG.error("Cross-encoder returned empty scores — falling back to dense-only");
                return List.of();
            }

            List<RerankedHit> hits = new ArrayList<>();
            for (CandidateDocument c : batch) {
                Double crossScore = crossScores.getOrDefault(c.memoryId(), null);
                if (crossScore != null) {
                    // Final score: weighted blend — 60% cross-encoder, 40% semantic
                    double finalScore = 0.6 * crossScore + 0.4 * c.semanticScore();
                    hits.add(new RerankedHit(c.memoryId(), c.semanticScore(), crossScore, finalScore));
                } else {
                    // Candidate not scored by cross-encoder → demote to bottom with semantic score
                    LOG.debugv("Cross-encoder did not score candidate {0}, demoting", c.memoryId());
                    hits.add(new RerankedHit(c.memoryId(), c.semanticScore(), 0.0, c.semanticScore() * 0.3));
                }
            }

            // Sort by finalScore descending
            hits.sort(Comparator.comparingDouble(RerankedHit::finalScore).reversed());

            // Limit to topK
            if (hits.size() > topK) {
                hits = hits.subList(0, topK);
            }

            LOG.debugv("Cross-encoder reranked {0} candidates → top-{1}", batch.size(), hits.size());
            return hits;

        } catch (TimeoutException e) {
            LOG.warnv("CROSS_ENCODER_TIMEOUT — reranker exceeded {0}s budget, falling back to dense-only",
                    timeout.toSeconds());
            return List.of();
        } catch (Exception e) {
            LOG.errorv(e, "CROSS_ENCODER_UNAVAILABLE — reranker failed");
            return List.of();
        }
    }

    /**
     * Executes the prompt with the configured timeout using a shared daemon thread.
     */
    private String executeWithTimeout(String prompt) throws Exception {
        Future<String> future = executor.submit((Callable<String>) () -> chatModel.generate(prompt));
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * Builds a cross-encoder entailment prompt evaluating up to 20 (query, document) pairs.
     */
    private String buildCrossEncoderPrompt(String query, List<CandidateDocument> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a search relevance evaluator. For each document below, assess how relevant
                it is to the query on a scale from 0.0 (completely irrelevant) to 1.0 (perfectly relevant).
                
                Query: %s
                
                Documents:
                """.formatted(query));

        for (int i = 0; i < candidates.size(); i++) {
            CandidateDocument c = candidates.get(i);
            // Truncate very long documents to 1500 chars to stay within token limits
            String content = c.content();
            if (content != null && content.length() > 1500) {
                content = content.substring(0, 1497) + "...";
            }
            sb.append("[").append(i).append("] id=").append(c.memoryId())
                    .append(": ").append(content != null ? content : "").append("\n");
        }

        sb.append("""
                
                Return a JSON object mapping each document id to its relevance score.
                Example: {"mem-abc": 0.92, "mem-def": 0.45}
                
                Return ONLY the JSON object, no other text.
                """);

        return sb.toString();
    }

    /**
     * Parses the cross-encoder response JSON into a map of memoryId → score.
     */
    private Map<String, Double> parseCrossEncoderResponse(String response) {
        try {
            String json = extractJsonObject(response);
            return MAPPER.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (JsonProcessingException e) {
            LOG.errorv(e, "Failed to parse cross-encoder response: {0}", response);
            return Map.of();
        }
    }

    /**
     * Extracts a JSON object from an LLM response that may be wrapped in markdown.
     */
    private String extractJsonObject(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```(?:json)?\\s*", "").replaceAll("```\\s*$", "");
        }
        cleaned = cleaned.trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
