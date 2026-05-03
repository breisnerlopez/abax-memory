package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.CaseStatus;
import com.btl.administrador.api.domain.Criticality;

import java.time.OffsetDateTime;
import java.util.List;

public record CaseResponse(
        String id,
        String origin,
        String title,
        String description,
        String priority,
        String domain,
        Criticality criticality,
        List<String> tags,
        List<String> participants,
        CaseStatus status,
        OffsetDateTime createdAt,
        String closureResult,
        String closureMemoryId,
        OffsetDateTime closedAt) {
}
