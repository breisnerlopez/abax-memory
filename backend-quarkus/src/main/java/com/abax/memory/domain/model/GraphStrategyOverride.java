package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.GraphEntryStrategy;

/**
 * Request-level override for graph expansion strategy — v2.1.0.
 *
 * <p>Carries the parsed values from {@code X-Graph-Strategy},
 * {@code X-Graph-K}, and {@code X-Graph-Threshold} HTTP headers.
 * When present, overrides the domain profile's default graph
 * entry strategy for a single search request.</p>
 *
 * <p>Precedence (highest to lowest):
 * <ol>
 *   <li>{@code entryPoints} in request body</li>
 *   <li>{@code X-Graph-*} headers (this object)</li>
 *   <li>{@code graphEntryStrategy} in domain profile</li>
 * </ol>
 * </p>
 *
 * <p>References: ADR-011, FT-V21-004.1</p>
 */
public class GraphStrategyOverride {

    private final GraphEntryStrategy strategy;
    private final Integer graphK;
    private final Double graphThreshold;

    public GraphStrategyOverride(GraphEntryStrategy strategy, Integer graphK, Double graphThreshold) {
        this.strategy = strategy;
        this.graphK = graphK;
        this.graphThreshold = graphThreshold;
    }

    public GraphEntryStrategy getStrategy() { return strategy; }
    public Integer getGraphK() { return graphK; }
    public Double getGraphThreshold() { return graphThreshold; }
}
