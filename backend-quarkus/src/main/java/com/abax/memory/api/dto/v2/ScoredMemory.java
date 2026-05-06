package com.abax.memory.api.dto.v2;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.Map;

/**
 * Wrapper around {@link MemoryResponse} that adds {@code score} and
 * {@code source} fields for unified search results.
 *
 * <p>The {@code source} indicates whether the result originated from
 * a vector similarity search ({@code "vector"}) or from graph
 * expansion ({@code "graph"}).</p>
 *
 * <p>New in v2.1.0: {@code scoreComponents} breaks down the composite
 * score into semantic, crossEncoder, lexical, and graph components.
 * {@code graphExpanded} flags graph-origin results, and
 * {@code pipeline} indicates which pipeline produced the result.</p>
 *
 * <p>Uses {@link JsonUnwrapped} so that serialized JSON is flat —
 * all {@link MemoryResponse} fields appear at the top level alongside
 * {@code source}.</p>
 *
 * <p>References: EP-005 v2, Unified Search, FT-V21-001.1, FT-V21-001.2</p>
 */
public class ScoredMemory {

    @JsonUnwrapped
    private MemoryResponse memory;

    private String source;
    private Map<String, Double> scoreComponents;
    private String pipeline;
    private boolean graphExpanded;

    public ScoredMemory() {
    }

    /**
     * Constructs a scored memory result.
     *
     * @param memory the underlying memory response (must include score)
     * @param source origin of this result: {@code "vector"} or {@code "graph"}
     */
    public ScoredMemory(MemoryResponse memory, String source) {
        this.memory = memory;
        this.source = source;
    }

    /**
     * Constructs with full v2.1.0 metadata.
     */
    public ScoredMemory(MemoryResponse memory, String source,
                         Map<String, Double> scoreComponents, String pipeline,
                         boolean graphExpanded) {
        this.memory = memory;
        this.source = source;
        this.scoreComponents = scoreComponents;
        this.pipeline = pipeline;
        this.graphExpanded = graphExpanded;
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public MemoryResponse getMemory() {
        return memory;
    }

    public void setMemory(MemoryResponse memory) {
        this.memory = memory;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Double> getScoreComponents() {
        return scoreComponents;
    }

    public void setScoreComponents(Map<String, Double> scoreComponents) {
        this.scoreComponents = scoreComponents;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isGraphExpanded() {
        return graphExpanded;
    }

    public void setGraphExpanded(boolean graphExpanded) {
        this.graphExpanded = graphExpanded;
    }

    /**
     * Convenience: delegates to the wrapped {@link MemoryResponse#score()}.
     */
    public Double getScore() {
        return memory != null ? memory.score() : null;
    }
}
