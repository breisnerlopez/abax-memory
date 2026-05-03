package com.btl.administrador.api.dto;

import java.util.List;

public record SearchFiltersRequest(
        List<String> domains,
        List<String> states,
        List<String> origins,
        List<String> types,
        List<String> tags,
        List<String> criticalities,
        Boolean includeArchived) {
}
