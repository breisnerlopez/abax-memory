package com.abax.memory.infrastructure.ai;

import com.abax.memory.domain.model.RerankedHit;
import com.abax.memory.domain.service.CrossEncoderService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CrossEncoderServiceImpl} — FT-V21-001.1.
 *
 * <p>Verifies reranking correctness, graceful degradation on timeout,
 * malformed response handling, and unavailable model fallback.</p>
 */
@DisplayName("CrossEncoderService")
class CrossEncoderServiceTest {

    private ChatLanguageModel chatModel;
    private CrossEncoderService service;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatLanguageModel.class);
        service = new CrossEncoderServiceImpl(chatModel);
    }

    @Test
    @DisplayName("should rerank candidates correctly with valid JSON response")
    void shouldRerankCandidatesWithValidResponse() {
        // Given
        String query = "database connection timeout";
        List<CrossEncoderService.CandidateDocument> candidates = List.of(
                new CrossEncoderService.CandidateDocument("mem-001", "PostgreSQL connection pool exhausted", 0.85),
                new CrossEncoderService.CandidateDocument("mem-002", "Lunch menu for Friday", 0.72),
                new CrossEncoderService.CandidateDocument("mem-003", "Connection timeout after 30s in nginx", 0.80)
        );

        String mockResponse = "{\"mem-001\": 0.95, \"mem-002\": 0.10, \"mem-003\": 0.88}";
        when(chatModel.generate(anyString())).thenReturn(mockResponse);

        // When
        List<RerankedHit> result = service.rerank(query, candidates, 3);

        // Then
        assertThat(result).hasSize(3);
        // mem-001 should be top (highest cross-encoder score)
        assertThat(result.get(0).memoryId()).isEqualTo("mem-001");
        assertThat(result.get(0).crossEncoderScore()).isEqualTo(0.95);
        assertThat(result.get(0).semanticScore()).isEqualTo(0.85);
        assertThat(result.get(0).finalScore()).isGreaterThan(0.9);
        // mem-002 should be bottom (irrelevant)
        assertThat(result.get(2).memoryId()).isEqualTo("mem-002");
        assertThat(result.get(2).crossEncoderScore()).isEqualTo(0.10);
        assertThat(result.get(2).finalScore()).isLessThan(0.5);
    }

    @Test
    @DisplayName("should return empty list on timeout (graceful degradation)")
    void shouldReturnEmptyOnTimeout() {
        // Given
        String query = "test query";
        List<CrossEncoderService.CandidateDocument> candidates = List.of(
                new CrossEncoderService.CandidateDocument("mem-001", "test content", 0.80)
        );

        // Simulate timeout by making the mock sleep longer than 2s
        when(chatModel.generate(anyString())).thenAnswer(invocation -> {
            Thread.sleep(2500); // exceeds 2s timeout
            return "{}";
        });

        // When
        List<RerankedHit> result = service.rerank(query, candidates, 3);

        // Then
        assertThat(result).isEmpty(); // graceful degradation → empty
    }

    @Test
    @DisplayName("should handle malformed JSON response gracefully")
    void shouldHandleMalformedJsonResponse() {
        // Given
        String query = "test query";
        List<CrossEncoderService.CandidateDocument> candidates = List.of(
                new CrossEncoderService.CandidateDocument("mem-001", "content one", 0.85),
                new CrossEncoderService.CandidateDocument("mem-002", "content two", 0.75)
        );

        String malformedResponse = "not json at all {{{";
        when(chatModel.generate(anyString())).thenReturn(malformedResponse);

        // When
        List<RerankedHit> result = service.rerank(query, candidates, 3);

        // Then — should return empty since no candidates could be scored
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should handle empty candidate list")
    void shouldHandleEmptyCandidates() {
        // Given
        String query = "test query";
        List<CrossEncoderService.CandidateDocument> emptyCandidates = List.of();

        // When
        List<RerankedHit> result = service.rerank(query, emptyCandidates, 3);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should limit results to topK")
    void shouldLimitResultsToTopK() {
        // Given
        String query = "test query";
        List<CrossEncoderService.CandidateDocument> candidates = List.of(
                new CrossEncoderService.CandidateDocument("mem-001", "A", 0.90),
                new CrossEncoderService.CandidateDocument("mem-002", "B", 0.85),
                new CrossEncoderService.CandidateDocument("mem-003", "C", 0.80),
                new CrossEncoderService.CandidateDocument("mem-004", "D", 0.75),
                new CrossEncoderService.CandidateDocument("mem-005", "E", 0.70)
        );

        String mockResponse = "{\"mem-001\":0.95,\"mem-002\":0.90,\"mem-003\":0.85,\"mem-004\":0.80,\"mem-005\":0.75}";
        when(chatModel.generate(anyString())).thenReturn(mockResponse);

        // When
        List<RerankedHit> result = service.rerank(query, candidates, 2);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).memoryId()).isEqualTo("mem-001");
        assertThat(result.get(1).memoryId()).isEqualTo("mem-002");
    }

    @Test
    @DisplayName("should demote candidates not scored by cross-encoder")
    void shouldDemoteUnscoredCandidates() {
        // Given
        String query = "test query";
        List<CrossEncoderService.CandidateDocument> candidates = List.of(
                new CrossEncoderService.CandidateDocument("mem-001", "highly relevant", 0.90),
                new CrossEncoderService.CandidateDocument("mem-002", "somewhat relevant", 0.75),
                new CrossEncoderService.CandidateDocument("mem-003", "maybe relevant", 0.60)
        );

        // Cross-encoder only scores mem-001 and mem-003
        String mockResponse = "{\"mem-001\": 0.95, \"mem-003\": 0.65}";
        when(chatModel.generate(anyString())).thenReturn(mockResponse);

        // When
        List<RerankedHit> result = service.rerank(query, candidates, 3);

        // Then
        assertThat(result).hasSize(3);
        // mem-001 should be first (cross-encoder confirms it)
        assertThat(result.get(0).memoryId()).isEqualTo("mem-001");
        // mem-002 was not scored by cross-encoder → demoted with semantic*0.3
        RerankedHit unscored = result.stream()
                .filter(h -> h.memoryId().equals("mem-002"))
                .findFirst().orElseThrow();
        assertThat(unscored.crossEncoderScore()).isEqualTo(0.0);
        assertThat(unscored.finalScore()).isEqualTo(0.75 * 0.3);
    }
}
