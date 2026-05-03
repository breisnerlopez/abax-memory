package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateCaseRequest(
        @NotBlank String origin,
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank @Pattern(
                regexp = "^(?i)(BAJA|MEDIA|ALTA|CRITICA)$",
                message = "priority must be one of: BAJA, MEDIA, ALTA, CRITICA") String priority,
        @NotBlank String domain,
        @NotNull Criticality criticality,
        List<String> tags,
        List<String> participants) {
}
