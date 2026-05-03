package com.btl.administrador.api.dto;

import com.btl.administrador.api.domain.Criticality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateMemoryRequest(
        @NotBlank String title,
        @NotBlank String type,
        @NotNull Criticality criticality,
        @NotEmpty List<String> domains,
        List<String> tags,
        @NotBlank String contenidoMarkdown,
        @NotNull Map<String, String> metadata,
        @NotNull Map<String, Object> frontmatter) {
}
