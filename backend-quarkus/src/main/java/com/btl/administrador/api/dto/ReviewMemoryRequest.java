package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.ReviewDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewMemoryRequest(
        @NotNull ReviewDecision decision,
        @NotBlank String comentario) {
}
