package com.abax.memory.infrastructure.ai;

import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.model.*;
import com.abax.memory.domain.service.LlmService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI-backed implementation of {@link LlmService} — v2.0.0.
 * <p>
 * Uses LangChain4j ChatLanguageModel beans for LLM calls:
 * <ul>
 *   <li>{@code gpt-4o-mini} for summarization, entity extraction, and relation inference</li>
 *   <li>{@code gpt-4o} for validation (as in v1 btl module)</li>
 * </ul>
 * </p>
 *
 * <p><strong>REAL INTEGRATION.</strong> Requires LangChain4j ChatLanguageModel CDI beans.</p>
 *
 * <p>References: FT-001.04, FT-005.05, HU-005.08.1</p>
 */
// @ApplicationScoped — managed by InfrastructureConfig.llmService() producer
public class OpenAiLlmService implements LlmService {

    private static final Logger LOG = Logger.getLogger(OpenAiLlmService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatLanguageModel chatModel;

    /**
     * Constructor with CDI injection of the langchain4j chat model.
     * Uses the single ChatLanguageModel bean (gpt-4o-mini by default).
     */
    public OpenAiLlmService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
        LOG.infov("OpenAiLlmService initialized: chatModel={0}", chatModel);
    }

    // ── Entity Extraction ─────────────────────────────────────────

    @Override
    public List<ExtractedEntity> extractEntities(String content, MemoryKind kind) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String prompt = buildEntityExtractionPrompt(content, kind);
        try {
            String response = chatModel.generate(prompt);
            LOG.debugv("Entity extraction response: {0}", response);
            return parseEntityResponse(response);
        } catch (Exception e) {
            LOG.errorv(e, "Entity extraction failed for kind={0}", kind);
            return List.of();
        }
    }

    private String buildEntityExtractionPrompt(String content, MemoryKind kind) {
        return """
                You are an entity extraction system. Extract all named entities from the text below.
                Return a JSON array of objects with fields: "name" (canonical entity name),
                "type" (one of: PERSON, SYSTEM, DATE, TICKET, METRIC, TECHNOLOGY, PLATFORM, TOOL, CUSTOM),
                and "confidence" (a number between 0.0 and 1.0).

                Memory kind: %s

                Text:
                %s

                Return ONLY the JSON array, no other text.
                Example: [{"name":"Kubernetes","type":"TECHNOLOGY","confidence":0.95}]
                """.formatted(kind.jsonValue(), content);
    }

    private List<ExtractedEntity> parseEntityResponse(String response) {
        try {
            String json = extractJsonArray(response);
            List<Map<String, Object>> raw = MAPPER.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            return raw.stream()
                    .map(m -> new ExtractedEntity(
                            (String) m.get("name"),
                            (String) m.getOrDefault("type", "CUSTOM"),
                            m.get("confidence") instanceof Number n ? n.doubleValue() : 0.5))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            LOG.warnv("Failed to parse entity extraction response: {0}", response);
            return List.of();
        }
    }

    // ── Relation Inference ────────────────────────────────────────

    @Override
    public List<InferredRelation> inferRelations(MemoryFragment fragment, List<MemoryFragment> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // Build a prompt with the source fragment and candidates
        StringBuilder sb = new StringBuilder();
        sb.append("Source fragment [").append(fragment.getId()).append("]: ")
                .append(fragment.getContent()).append("\n\n");
        sb.append("Candidate fragments:\n");
        for (int i = 0; i < Math.min(candidates.size(), 20); i++) {
            var c = candidates.get(i);
            sb.append("[").append(c.getId()).append("]: ").append(c.getContent()).append("\n");
        }

        String prompt = """
                Analyze the source fragment and candidate fragments below.
                Identify meaningful relationships between the source and each candidate.
                Return a JSON array with fields: "targetId" (UUID string), "relationType"
                (one of: related_to, depends_on, caused_by, resolves, contradicts, supports, mentions, belongs_to, supersedes),
                "confidence" (0.0 to 1.0), and "evidence" (brief explanation).

                Only include relationships that are clearly supported by the content.
                If no meaningful relations exist, return an empty array [].

                %s

                Return ONLY the JSON array.
                """.formatted(sb.toString());

        try {
            String response = chatModel.generate(prompt);
            return parseRelationResponse(response, fragment.getId());
        } catch (Exception e) {
            LOG.errorv(e, "Relation inference failed for fragment {0}", fragment.getId());
            return List.of();
        }
    }

    private List<InferredRelation> parseRelationResponse(String response, java.util.UUID sourceId) {
        try {
            String json = extractJsonArray(response);
            List<Map<String, Object>> raw = MAPPER.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            return raw.stream()
                    .map(m -> new InferredRelation(
                            sourceId,
                            java.util.UUID.fromString((String) m.get("targetId")),
                            com.abax.memory.domain.enums.RelationType.fromJson((String) m.get("relationType")),
                            m.get("confidence") instanceof Number n ? n.doubleValue() : 0.5,
                            (String) m.getOrDefault("evidence", "")))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warnv(e, "Failed to parse relation inference response: {0}", response);
            return List.of();
        }
    }

    // ── Summary Generation ────────────────────────────────────────

    @Override
    public String generateSummary(String content, MemoryKind kind) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String prompt = """
                Summarize the following text in 2-3 concise sentences.
                Focus on the key facts, decisions, or actions described.
                Memory kind: %s

                Text:
                %s

                Summary:
                """.formatted(kind.jsonValue(), content);

        try {
            String summary = chatModel.generate(prompt);
            LOG.debugv("Summary generated: length={0}", summary.length());
            return summary.trim();
        } catch (Exception e) {
            LOG.errorv(e, "Summary generation failed for kind={0}", kind);
            return content.length() > 200 ? content.substring(0, 197) + "..." : content;
        }
    }

    // ── Validation ────────────────────────────────────────────────

    @Override
    public ValidationResult validateMemory(MemoryFragment fragment) {
        String prompt = """
                Validate the following memory fragment for quality and consistency.
                Check for:
                1. Coherence: Does the content make sense?
                2. Completeness: Is there enough information?
                3. Duplication: Does the summary match the content?
                4. Lifecycle suggestion: draft, pending, active, rejected, archived, or deleted?
                5. Kind suggestion: fact, preference, event, decision, task, procedure, note, or entity?

                Current kind: %s
                Title: %s
                Content: %s

                Return a JSON object with fields:
                "isValid" (boolean), "issues" (array of strings), "suggestedLifecycle" (string or null),
                "suggestedKind" (string or null), "duplicateOf" (null).

                Return ONLY the JSON object.
                """.formatted(
                        fragment.getKind() != null ? fragment.getKind().jsonValue() : "unknown",
                        fragment.getTitle(),
                        fragment.getContent());

        try {
            String response = chatModel.generate(prompt);
            return parseValidationResponse(response);
        } catch (Exception e) {
            LOG.errorv(e, "Validation failed for fragment title={0}", fragment.getTitle());
            return ValidationResult.valid(); // graceful degradation
        }
    }

    private ValidationResult parseValidationResponse(String response) {
        try {
            String json = extractJsonObject(response);
            Map<String, Object> map = MAPPER.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            boolean isValid = Boolean.TRUE.equals(map.get("isValid"));
            @SuppressWarnings("unchecked")
            List<String> issues = map.get("issues") instanceof List<?> list
                    ? list.stream().map(Object::toString).collect(Collectors.toList())
                    : List.of();

            com.abax.memory.domain.enums.LifecycleState suggestedLifecycle = null;
            if (map.get("suggestedLifecycle") instanceof String s && !s.isBlank()) {
                try {
                    suggestedLifecycle = com.abax.memory.domain.enums.LifecycleState.fromJson(s);
                } catch (Exception ignored) { }
            }

            MemoryKind suggestedKind = null;
            if (map.get("suggestedKind") instanceof String s && !s.isBlank()) {
                try {
                    suggestedKind = MemoryKind.fromJson(s);
                } catch (Exception ignored) { }
            }

            return new ValidationResult(isValid, issues, suggestedLifecycle, suggestedKind, null);
        } catch (Exception e) {
            LOG.warnv(e, "Failed to parse validation response: {0}", response);
            return ValidationResult.valid();
        }
    }

    // ── Confidence Estimation ─────────────────────────────────────

    @Override
    public float estimateConfidence(String content, MemoryKind kind) {
        if (content == null || content.isBlank()) {
            return 0.0f;
        }

        String prompt = """
                Rate the confidence of the following text on a scale from 0.0 to 1.0,
                where 1.0 means highly specific, verifiable, and well-structured content,
                and 0.0 means vague, ambiguous, or unsubstantiated.

                Memory kind: %s

                Text:
                %s

                Return ONLY a number between 0.0 and 1.0 (e.g., 0.85). No other text.
                """.formatted(kind.jsonValue(), content);

        try {
            String response = chatModel.generate(prompt).trim();
            // Extract the first number from the response
            String number = response.replaceAll("[^0-9.]", "");
            if (!number.isEmpty()) {
                float confidence = Float.parseFloat(number);
                return Math.max(0.0f, Math.min(1.0f, confidence));
            }
        } catch (Exception e) {
            LOG.errorv(e, "Confidence estimation failed for kind={0}", kind);
        }
        return 0.5f;
    }

    // ── JSON helpers ──────────────────────────────────────────────

    /**
     * Extracts a JSON array from an LLM response that may be wrapped in markdown.
     */
    private String extractJsonArray(String response) {
        String cleaned = response.trim();
        // Remove markdown code fences if present
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```(?:json)?\\s*", "").replaceAll("```\\s*$", "");
        }
        cleaned = cleaned.trim();
        // Find the JSON array boundaries
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
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
