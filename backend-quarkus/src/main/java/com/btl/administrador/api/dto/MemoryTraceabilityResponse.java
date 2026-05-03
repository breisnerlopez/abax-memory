package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.MemoryOrigin;
import com.btl.administrador.api.domain.MemoryState;
import com.btl.administrador.api.domain.ProcessingStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record MemoryTraceabilityResponse(
        String memoryId,
        MemoryOrigin origin,
        String sourceCaseId,
        MemoryState state,
        ProcessingStatus processingStatus,
        String versionId,
        String commitSha,
        String pullRequestRef,
        String createdBy,
        OffsetDateTime createdAt,
        String lastModifiedBy,
        OffsetDateTime lastModifiedAt,
        List<AuditEventResponse> events) {
}
