package com.btl.administrador.api.dto;

import java.time.OffsetDateTime;

public record AuditEventResponse(
        String id,
        String entityId,
        String entityType,
        String action,
        String detail,
        String actor,
        String commitSha,
        String pullRequestRef,
        String correlationId,
        OffsetDateTime createdAt) {
}
