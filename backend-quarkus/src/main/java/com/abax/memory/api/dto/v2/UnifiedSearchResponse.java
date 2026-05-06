package com.abax.memory.api.dto.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for unified search (POST /api/v2/search).
 *
 * <p>Returns a merged, score-sorted list of results from both vector
 * search and graph expansion. The {@code graphExpanded} and
 * {@code graphContributions} fields give the client transparency
 * into how the result was composed without breaking the unified
 * abstraction.</p>
 *
 * <p>New in v2.1.0: {@code pipeline} metadata exposes the stages
 * executed, reranker status, dense retrieval candidate count, and
 * graph expansion details for full observability.</p>
 *
 * <p>References: EP-005 v2, Unified Search, FT-V21-001.1, FT-V21-001.3</p>
 */
@RegisterForReflection
public class UnifiedSearchResponse {

    private List<ScoredMemory> items;
    private long total;
    private int page;
    private int size;
    private boolean graphExpanded;
    private int graphContributions;
    private Map<String, Map<String, Long>> facets;
    private long queryTimeMs;
    private PipelineMetadata pipeline;

    public UnifiedSearchResponse() {
    }

    public UnifiedSearchResponse(List<ScoredMemory> items, long total,
                                   int page, int size,
                                   boolean graphExpanded, int graphContributions,
                                   Map<String, Map<String, Long>> facets) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.graphExpanded = graphExpanded;
        this.graphContributions = graphContributions;
        this.facets = facets;
    }

    /**
     * Full constructor with v2.1.0 pipeline metadata.
     */
    public UnifiedSearchResponse(List<ScoredMemory> items, long total,
                                   int page, int size,
                                   boolean graphExpanded, int graphContributions,
                                   Map<String, Map<String, Long>> facets,
                                   long queryTimeMs, PipelineMetadata pipeline) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.graphExpanded = graphExpanded;
        this.graphContributions = graphContributions;
        this.facets = facets;
        this.queryTimeMs = queryTimeMs;
        this.pipeline = pipeline;
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public List<ScoredMemory> getItems() { return items; }
    public void setItems(List<ScoredMemory> items) { this.items = items; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public boolean isGraphExpanded() { return graphExpanded; }
    public void setGraphExpanded(boolean graphExpanded) { this.graphExpanded = graphExpanded; }

    public int getGraphContributions() { return graphContributions; }
    public void setGraphContributions(int graphContributions) { this.graphContributions = graphContributions; }

    public Map<String, Map<String, Long>> getFacets() { return facets; }
    public void setFacets(Map<String, Map<String, Long>> facets) { this.facets = facets; }

    public long getQueryTimeMs() { return queryTimeMs; }
    public void setQueryTimeMs(long queryTimeMs) { this.queryTimeMs = queryTimeMs; }

    public PipelineMetadata getPipeline() { return pipeline; }
    public void setPipeline(PipelineMetadata pipeline) { this.pipeline = pipeline; }

    /**
     * Metadata about the search pipeline execution — v2.1.0.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PipelineMetadata {
        private List<String> stages;
        private boolean crossEncoderApplied;
        private int denseRetrievalCandidates;
        private boolean graphExpanded;
        private GraphExpandedNodes graphExpandedNodes;
        // FT-V21-004.2: search weights
        private Double semanticWeight;
        private Double lexicalWeight;

        public PipelineMetadata() {}

        public PipelineMetadata(List<String> stages, boolean crossEncoderApplied,
                                  int denseRetrievalCandidates, boolean graphExpanded,
                                  GraphExpandedNodes graphExpandedNodes) {
            this.stages = stages;
            this.crossEncoderApplied = crossEncoderApplied;
            this.denseRetrievalCandidates = denseRetrievalCandidates;
            this.graphExpanded = graphExpanded;
            this.graphExpandedNodes = graphExpandedNodes;
        }

        public PipelineMetadata(List<String> stages, boolean crossEncoderApplied,
                                  int denseRetrievalCandidates, boolean graphExpanded,
                                  GraphExpandedNodes graphExpandedNodes,
                                  double semanticWeight, double lexicalWeight) {
            this(stages, crossEncoderApplied, denseRetrievalCandidates, graphExpanded, graphExpandedNodes);
            this.semanticWeight = semanticWeight;
            this.lexicalWeight = lexicalWeight;
        }

        public List<String> getStages() { return stages; }
        public void setStages(List<String> stages) { this.stages = stages; }

        public boolean isCrossEncoderApplied() { return crossEncoderApplied; }
        public void setCrossEncoderApplied(boolean crossEncoderApplied) { this.crossEncoderApplied = crossEncoderApplied; }

        public int getDenseRetrievalCandidates() { return denseRetrievalCandidates; }
        public void setDenseRetrievalCandidates(int denseRetrievalCandidates) { this.denseRetrievalCandidates = denseRetrievalCandidates; }

        public boolean isGraphExpanded() { return graphExpanded; }
        public void setGraphExpanded(boolean graphExpanded) { this.graphExpanded = graphExpanded; }

        public GraphExpandedNodes getGraphExpandedNodes() { return graphExpandedNodes; }
        public void setGraphExpandedNodes(GraphExpandedNodes graphExpandedNodes) { this.graphExpandedNodes = graphExpandedNodes; }

        public Double getSemanticWeight() { return semanticWeight; }
        public void setSemanticWeight(Double semanticWeight) { this.semanticWeight = semanticWeight; }
        public Double getLexicalWeight() { return lexicalWeight; }
        public void setLexicalWeight(Double lexicalWeight) { this.lexicalWeight = lexicalWeight; }
    }

    /**
     * Details of the graph expansion phase — v2.1.0.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GraphExpandedNodes {
        private List<String> entryPointIds;
        private int entryPointCount;
        private String entryPointSource;
        private int totalExpandedNodes;
        private int maxDepth;
        private boolean cacheHit;

        public GraphExpandedNodes() {}

        public GraphExpandedNodes(List<String> entryPointIds, int entryPointCount,
                                   String entryPointSource, int totalExpandedNodes,
                                   int maxDepth, boolean cacheHit) {
            this.entryPointIds = entryPointIds;
            this.entryPointCount = entryPointCount;
            this.entryPointSource = entryPointSource;
            this.totalExpandedNodes = totalExpandedNodes;
            this.maxDepth = maxDepth;
            this.cacheHit = cacheHit;
        }

        public List<String> getEntryPointIds() { return entryPointIds; }
        public void setEntryPointIds(List<String> entryPointIds) { this.entryPointIds = entryPointIds; }

        public int getEntryPointCount() { return entryPointCount; }
        public void setEntryPointCount(int entryPointCount) { this.entryPointCount = entryPointCount; }

        public String getEntryPointSource() { return entryPointSource; }
        public void setEntryPointSource(String entryPointSource) { this.entryPointSource = entryPointSource; }

        public int getTotalExpandedNodes() { return totalExpandedNodes; }
        public void setTotalExpandedNodes(int totalExpandedNodes) { this.totalExpandedNodes = totalExpandedNodes; }

        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

        public boolean isCacheHit() { return cacheHit; }
        public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    }
}
