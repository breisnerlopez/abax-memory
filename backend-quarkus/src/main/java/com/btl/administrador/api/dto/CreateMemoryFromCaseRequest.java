package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateMemoryFromCaseRequest(
        @NotBlank String caseId,
        @NotBlank String title,
        @NotBlank String type,
        @NotNull Criticality criticality,
        @NotEmpty List<String> domains,
        List<String> tags,
        Map<String, String> metadata,
        Map<String, Object> frontmatter) {
}
