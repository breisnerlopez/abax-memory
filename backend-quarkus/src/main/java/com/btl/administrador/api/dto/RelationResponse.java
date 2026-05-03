package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.RelationType;

import java.time.OffsetDateTime;

public record RelationResponse(
        String id,
        String sourceMemoryId,
        String targetMemoryId,
        RelationType relationType,
        OffsetDateTime createdAt) {
}
