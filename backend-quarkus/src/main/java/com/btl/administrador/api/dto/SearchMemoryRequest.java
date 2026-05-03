package com.btl.administrador.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SearchMemoryRequest(
        @NotBlank String consulta,
        @NotNull Integer topK,
        SearchFiltersRequest filtros) {
}
