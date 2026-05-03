package com.btl.administrador.api.integration.qdrant;

import com.btl.administrador.api.dto.SearchFiltersRequest;
import com.btl.administrador.api.service.model.SearchHit;

import java.util.List;

public interface SearchIndexer {
    void index(String memoryId, String title, String markdown);

    List<SearchHit> search(String query, int topK, SearchFiltersRequest filters);

    void clear();
}
