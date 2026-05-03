package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.Criticality;
import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.ProcessingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record MemoryResponse(
        String id,
        String title,
        String type,
        MemoryOrigin origin,
        String sourceCaseId,
        Criticality criticality,
        MemoryState state,
        ProcessingStatus processingStatus,
        List<String> domains,
        List<String> tags,
        Map<String, String> metadata,
        String versionId,
        String markdown,
        String commitSha,
        String pullRequestRef,
        String canonicalMemoryId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RelationResponse> relations) {
}
