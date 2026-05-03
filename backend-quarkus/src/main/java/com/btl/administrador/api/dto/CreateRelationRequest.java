package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.RelationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRelationRequest(
        @NotBlank String targetMemoryId,
        @NotNull RelationType relationType) {
}
