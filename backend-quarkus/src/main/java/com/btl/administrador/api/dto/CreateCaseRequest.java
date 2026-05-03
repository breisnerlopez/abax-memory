package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCaseRequest(
        @NotBlank String origin,
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String priority,
        @NotBlank String domain,
        @NotNull Criticality criticality,
        List<String> tags,
        List<String> participants) {
}
