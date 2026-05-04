package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import java.time.Instant;
import java.util.List;

/**
 * Request DTO for GET /api/v2/memories — filtered, paginated listing.
 *
 * <p>{@code query} is the only required parameter. All others are
 * optional filters that narrow the result set.</p>
 *
 * <p><b>Note:</b> This is a plain Java class (not a record) because
 * Quarkus Arc cannot correctly handle JAX-RS annotations on record
 * components — it attempts CDI injection on the constructor parameters.</p>
 *
 * <p>References: HU-004.5.1, API Design §7.2</p>
 */
public class SearchRequest {

    @NotBlank(message = "query is required")
    @QueryParam("query")
    private String query;

    @QueryParam("kind")
    private List<MemoryKind> kinds;

    @QueryParam("lifecycle_state")
    private List<LifecycleState> lifecycleStates;

    @QueryParam("sensitivity_max")
    private SensitivityLevel sensitivityMax;

    @QueryParam("scope_id")
    private List<String> scopeIds;

    @QueryParam("from_date")
    private Instant fromDate;

    @QueryParam("to_date")
    private Instant toDate;

    @QueryParam("page")
    @DefaultValue("0")
    private int page;

    @QueryParam("size")
    @DefaultValue("20")
    private int size;

    public SearchRequest() {
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

    // ── Convenience constructor for tests ───────────────────────────

    public SearchRequest(String query, List<MemoryKind> kinds, List<LifecycleState> lifecycleStates,
                         SensitivityLevel sensitivityMax, List<String> scopeIds,
                         Instant fromDate, Instant toDate, int page, int size) {
        this.query = query;
        this.kinds = kinds;
        this.lifecycleStates = lifecycleStates;
        this.sensitivityMax = sensitivityMax;
        this.scopeIds = scopeIds;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.page = page;
        this.size = size;
    }
}
