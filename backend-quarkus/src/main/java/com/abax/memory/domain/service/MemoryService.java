package com.abax.memory.domain.service;

import com.abax.memory.domain.model.MemoryFragment;

import java.util.Optional;
import java.util.UUID;

/**
 * Core service contract for MemoryFragment CRUD operations — v2.0.0.
 * <p>
 * Implementation will handle validation, lifecycle transitions,
 * audit-recording, and processing-job scheduling.
 * </p>
 */
public interface MemoryService {

    /**
     * Creates a new memory fragment, assigns a UUID, sets initial
     * lifecycle state, and records an audit event.
     */
    MemoryFragment create(MemoryFragment fragment);

    /**
     * Retrieves a memory fragment by its id, scoped to the
     * current tenant.
     */
    Optional<MemoryFragment> findById(UUID id);

    /**
     * Updates mutable fields of an existing memory fragment.
     * Returns the updated fragment.
     */
    MemoryFragment update(UUID id, MemoryFragment updates);

    /**
     * Soft-deletes a memory fragment (sets {@code deletedAt} and
     * transitions lifecycle to {@code DELETED}).
     */
    void softDelete(UUID id);
}
