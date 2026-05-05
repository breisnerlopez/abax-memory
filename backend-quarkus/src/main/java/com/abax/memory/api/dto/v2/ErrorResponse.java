package com.abax.memory.api.dto.v2;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response body for all API v2 error responses.
 *
 * <p>Follows the error format defined in API Design §7.3.</p>
 *
 * <p>References: Architecture document §7.3, FT-001.10</p>
 */
public record ErrorResponse(
        String errorCode,
        String message,
        List<String> details,
        Instant timestamp,
        String path
) {

    public static ErrorResponse of(String errorCode, String message, List<String> details, String path) {
        return new ErrorResponse(errorCode, message, details, Instant.now(), path);
    }

    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(errorCode, message, List.of(), Instant.now(), path);
    }
}
