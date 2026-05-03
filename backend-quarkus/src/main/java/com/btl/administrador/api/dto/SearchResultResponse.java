package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryState;

import java.util.List;

public record SearchResultResponse(
        String memoryId,
        String title,
        String summary,
        double score,
        MemoryState state,
        MemoryOrigin origin,
        Criticality criticality,
        List<String> domains,
        List<String> tags,
        String commitSha) {
}
