package com.btl.administrador.api.dto;

import java.util.List;
import java.util.Map;

public record UpdateMemoryRequest(
        String title,
        String type,
        List<String> domains,
        List<String> tags,
        String contenidoMarkdown,
        Map<String, String> metadata,
        Map<String, Object> frontmatter) {
}
