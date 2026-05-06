package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request DTO for POST /api/v2/search — unified search combining
 * vector similarity and graph expansion transparently.
 *
 * <p>Extends the basic search parameters with graph-expansion
 * controls. When {@code expandGraph} is {@code true}, the search
 * engine expands the relationship graph from the top-K vector
 * results and merges connected nodes into the result set.</p>
 *
 * <p>References: EP-005 v2, Unified Search</p>
 */
@RegisterForReflection
public class UnifiedSearchRequest {

    @NotBlank(message = "query is required")
    private String query;

    private List<MemoryKind> kinds;
    private List<LifecycleState> lifecycleStates;
    private SensitivityLevel sensitivityMax;
    private List<String> scopeIds;

    private int page = 0;
    private int size = 20;

    // FT-V21-001.2: expandGraph default changed from true → false.
    // When false, the search is pure semantic (dense + cross-encoder)
    // with zero graph contributions.
    private boolean expandGraph = false;
    private int graphDepth = 2;
    // FT-V21-001.3: graphTopK default changed from 5 → 3 for multi-origin expansion.
    private int graphTopK = 3;
    // FT-V21-001.1: enables/disables the cross-encoder reranker stage.
    private boolean rerank = true;
    // FT-V21-001.3: explicit entry points for graph expansion (bypass semantic).
    private List<String> entryPoints;
    // FT-V21-004.2: semantic and lexical weights for hybrid search unification.
    private double semanticWeight = 1.0;
    private double lexicalWeight = 0.0;

    public UnifiedSearchRequest() {
    }

    // ── Convenience constructor for tests ───────────────────────────

    public UnifiedSearchRequest(String query, List<MemoryKind> kinds,
                                 List<LifecycleState> lifecycleStates,
                                 SensitivityLevel sensitivityMax,
                                 List<String> scopeIds,
                                 int page, int size,
                                 boolean expandGraph, int graphDepth, int graphTopK) {
        this.query = query;
        this.kinds = kinds;
        this.lifecycleStates = lifecycleStates;
        this.sensitivityMax = sensitivityMax;
        this.scopeIds = scopeIds;
        this.page = page;
        this.size = size;
        this.expandGraph = expandGraph;
        this.graphDepth = graphDepth;
        this.graphTopK = graphTopK;
    }

    // ── Getters / Setters ───────────────────────────────────────────

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<MemoryKind> getKinds() { return kinds; }
    public void setKinds(List<MemoryKind> kinds) { this.kinds = kinds; }

    public List<LifecycleState> getLifecycleStates() { return lifecycleStates; }
    public void setLifecycleStates(List<LifecycleState> lifecycleStates) { this.lifecycleStates = lifecycleStates; }

    public SensitivityLevel getSensitivityMax() { return sensitivityMax; }
    public void setSensitivityMax(SensitivityLevel sensitivityMax) { this.sensitivityMax = sensitivityMax; }

    public List<String> getScopeIds() { return scopeIds; }
    public void setScopeIds(List<String> scopeIds) { this.scopeIds = scopeIds; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public boolean isExpandGraph() { return expandGraph; }
    public void setExpandGraph(boolean expandGraph) { this.expandGraph = expandGraph; }

    public int getGraphDepth() { return graphDepth; }
    public void setGraphDepth(int graphDepth) { this.graphDepth = graphDepth; }

    public int getGraphTopK() { return graphTopK; }
    public void setGraphTopK(int graphTopK) { this.graphTopK = graphTopK; }

    public boolean isRerank() { return rerank; }
    public void setRerank(boolean rerank) { this.rerank = rerank; }

    public List<String> getEntryPoints() { return entryPoints; }
    public void setEntryPoints(List<String> entryPoints) { this.entryPoints = entryPoints; }

    public double getSemanticWeight() { return semanticWeight; }
    public void setSemanticWeight(double semanticWeight) { this.semanticWeight = semanticWeight; }

    public double getLexicalWeight() { return lexicalWeight; }
    public void setLexicalWeight(double lexicalWeight) { this.lexicalWeight = lexicalWeight; }
}
