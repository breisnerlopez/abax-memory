package com.abax.memory.api.dto.v2;

import java.util.List;

/**
 * Generic paginated response wrapper for list endpoints.
 *
 * <p>{@code hasNext} is derived from total, page, and size.</p>
 *
 * <p>References: API Design §7.1</p>
 */
public record PaginatedResponse<T>(
        List<T> items,
        long total,
        int page,
        int size,
        boolean hasNext
) {

    /**
     * Creates a PaginatedResponse from a list and pagination params.
     */
    public static <T> PaginatedResponse<T> of(List<T> items, long total, int page, int size) {
        return new PaginatedResponse<>(items, total, page, size, (long) (page + 1) * size < total);
    }
}
