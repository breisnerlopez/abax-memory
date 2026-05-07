package com.btl.administrador.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        String errorCode,
        String message,
        String correlationId,
        List<String> details,
        OffsetDateTime timestamp) {
}
