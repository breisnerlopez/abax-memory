package com.abax.memory.api.dto.v2;

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
 * <p>References: EP-005 v2, Unified Search</p>
 */
public class UnifiedSearchResponse {

    private List<ScoredMemory> items;
    private long total;
    private int page;
    private int size;
    private boolean graphExpanded;
    private int graphContributions;
    private Map<String, Map<String, Long>> facets;

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
}
