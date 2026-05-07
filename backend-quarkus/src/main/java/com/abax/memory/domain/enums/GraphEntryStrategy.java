package com.abax.memory.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Strategy for selecting graph entry points from dense retrieval results — v2.1.0.
 *
 * <p>Controls how the search engine picks seed nodes for BFS-based graph expansion.
 * Configurable per domain profile (FT-V21-003.3) or per request via
 * {@code X-Graph-Strategy} header (FT-V21-004.1).</p>
 *
 * <p>References: ADR-010, FT-V21-003.3, FT-V21-004.1</p>
 */
public enum GraphEntryStrategy {

    /** Expand only from the single best semantic match (v2.0.9 behavior). */
    SINGLE_BEST,

    /** Expand from the top-K semantic matches (default K=3 in v2.1.0). */
    TOP_K,

    /** Expand from all semantic matches with score ≥ threshold. */
    THRESHOLD;

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase().replace('_', '-');
    }

    @JsonCreator
    public static GraphEntryStrategy fromJson(String value) {
        if (value == null || value.isBlank()) {
            return TOP_K; // default
        }
        String normalized = value.trim().toLowerCase().replace('-', '_');
        for (GraphEntryStrategy s : values()) {
            if (s.name().equalsIgnoreCase(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException(
                "Unknown GraphEntryStrategy: " + value
                + ". Supported: single-best, top-k, threshold");
    }
}
