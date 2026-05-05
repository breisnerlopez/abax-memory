package com.abax.memory.infrastructure.ai;

import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.model.*;
import com.abax.memory.domain.service.LlmService;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Mock implementation of {@link LlmService} for tests.
 * Produces predictable, deterministic responses without any LLM call.
 *
 * <p><strong>ONLY FOR TESTS.</strong> Activated when
 * {@code abax.v2.llm.mock=true} in application.properties.</p>
 *
 * <p>NOTE: This class is intentionally NOT annotated with
 * {@code @ApplicationScoped}. It is produced by
 * {@code InfrastructureConfig.llmService()} to avoid CDI ambiguity
 * with {@link OpenAiLlmService}.</p>
 *
 * <p>References: Incident Abax-Memory v2 (mayo 2026) — mock service
 * pattern to avoid OpenAI timeouts in CI/CD</p>
 */
public class MockLlmService implements LlmService {

    private static final Logger LOG = Logger.getLogger(MockLlmService.class);

    public MockLlmService() {
        LOG.warn("MockLlmService ACTIVE — all LLM calls return deterministic test data. "
                + "REPLACE_BEFORE_PROD with OpenAiLlmService.");
    }

    @Override
    public List<ExtractedEntity> extractEntities(String content, MemoryKind kind) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var entities = new java.util.ArrayList<ExtractedEntity>();
        String lower = content.toLowerCase();

        if (lower.contains("kubernetes") || lower.contains("k8s")) {
            entities.add(new ExtractedEntity("Kubernetes", "SYSTEM", 0.95));
        }
        if (lower.contains("postgres") || lower.contains("postgresql")) {
            entities.add(new ExtractedEntity("PostgreSQL", "SYSTEM", 0.93));
        }
        if (lower.contains("incident") || lower.contains("outage")) {
            entities.add(new ExtractedEntity("Incident", "CUSTOM", 0.88));
        }
        if (lower.contains("deploy") || lower.contains("deployment")) {
            entities.add(new ExtractedEntity("Deployment", "CUSTOM", 0.90));
        }
        if (lower.contains("error") || lower.contains("failure")) {
            entities.add(new ExtractedEntity("Error", "METRIC", 0.85));
        }
        if (lower.contains("202") || lower.contains("jan") || lower.contains("feb")) {
            entities.add(new ExtractedEntity("2024-01-15", "DATE", 0.97));
        }
        if (lower.contains("john") || lower.contains("admin")) {
            entities.add(new ExtractedEntity("John Doe", "PERSON", 0.92));
        }
        if (lower.contains("ticket") || lower.contains("jira")) {
            entities.add(new ExtractedEntity("JIRA-1234", "TICKET", 0.94));
        }

        // Always return at least one entity for non-empty content
        if (entities.isEmpty()) {
            entities.add(new ExtractedEntity("Test Entity", "CUSTOM", 0.75));
        }

        return entities;
    }

    @Override
    public List<InferredRelation> inferRelations(MemoryFragment fragment, List<MemoryFragment> candidates) {
        // Mock: no automatic relation inference in tests.
        // Tests create explicit relations and should not be surprised by inferred ones.
        return List.of();
    }

    @Override
    public String generateSummary(String content, MemoryKind kind) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        return "Test summary for: " + preview;
    }

    @Override
    public ValidationResult validateMemory(MemoryFragment fragment) {
        return new ValidationResult(
                true,
                List.of(),
                com.abax.memory.domain.enums.LifecycleState.DRAFT,
                MemoryKind.FACT,
                null
        );
    }

    @Override
    public float estimateConfidence(String content, MemoryKind kind) {
        if (content == null || content.isBlank()) {
            return 0.0f;
        }
        // Return 0.5 to match the system default — avoids breaking tests
        // that expect the default confidence to remain unchanged.
        return 0.5f;
    }
}
