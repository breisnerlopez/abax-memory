package com.btl.administrador.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseCaseRequest(
        @NotBlank String resultadoOperativo,
        String memoryId,
        String observaciones) {
}
