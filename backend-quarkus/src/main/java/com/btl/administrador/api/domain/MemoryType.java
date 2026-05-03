package com.btl.administrador.api.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum MemoryType {
    PROCEDIMIENTO,
    RUNBOOK,
    INCIDENTE,
    POLITICA,
    CASO,
    GUIA;

    public static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }

        try {
            return MemoryType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT)).name().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unsupported memory type");
        }
    }

    public static Set<String> allowedValues() {
        return Arrays.stream(values())
                .map(value -> value.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
