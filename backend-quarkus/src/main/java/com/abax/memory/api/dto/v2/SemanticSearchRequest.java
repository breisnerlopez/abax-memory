package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for POST /api/v2/search/semantic and /api/v2/search/hybrid.
 *
 * <p>Uses a POST body with a JSON payload for richer filter expression.
 * This is a plain JSON body class — NOT a {@code @BeanParam} — so
 * JAX-RS annotations are not present.</p>
 *
 * <p>References: HU-005.1.1, HU-005.2.1</p>
 */
public class SemanticSearchRequest {

    @NotBlank(message = "query is required")
    private String query;

    private List<MemoryKind> kinds;
    private List<LifecycleState> lifecycleStates;
    private SensitivityLevel sensitivityMax;
    private List<String> scopeIds;
    private Instant fromDate;
    private Instant toDate;

    private int page = 0;
    private int size = 20;
    private int topK = 10;

    public SemanticSearchRequest() {
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

    public Instant getFromDate() { return fromDate; }
    public void setFromDate(Instant fromDate) { this.fromDate = fromDate; }

    public Instant getToDate() { return toDate; }
    public void setToDate(Instant toDate) { this.toDate = toDate; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    // ── Convenience constructor for tests ───────────────────────────

    public SemanticSearchRequest(String query, List<MemoryKind> kinds, List<LifecycleState> lifecycleStates,
                                  SensitivityLevel sensitivityMax, List<String> scopeIds,
                                  Instant fromDate, Instant toDate, int page, int size, int topK) {
        this.query = query;
        this.kinds = kinds;
        this.lifecycleStates = lifecycleStates;
        this.sensitivityMax = sensitivityMax;
        this.scopeIds = scopeIds;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.page = page;
        this.size = size;
        this.topK = topK;
    }
}
