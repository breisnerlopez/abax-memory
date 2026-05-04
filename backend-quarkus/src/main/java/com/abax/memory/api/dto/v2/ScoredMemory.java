package com.abax.memory.api.dto.v2;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Wrapper around {@link MemoryResponse} that adds {@code score} and
 * {@code source} fields for unified search results.
 *
 * <p>The {@code source} indicates whether the result originated from
 * a vector similarity search ({@code "vector"}) or from graph
 * expansion ({@code "graph"}).</p>
 *
 * <p>Uses {@link JsonUnwrapped} so that serialized JSON is flat —
 * all {@link MemoryResponse} fields appear at the top level alongside
 * {@code source}.</p>
 *
 * <p>References: EP-005 v2, Unified Search</p>
 */
public class ScoredMemory {

    @JsonUnwrapped
    private MemoryResponse memory;

    private String source;

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

    /**
     * Convenience: delegates to the wrapped {@link MemoryResponse#score()}.
     */
    public Double getScore() {
        return memory != null ? memory.score() : null;
    }
}
