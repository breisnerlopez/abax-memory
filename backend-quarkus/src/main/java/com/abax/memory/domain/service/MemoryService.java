package com.abax.memory.domain.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchRequest;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.UpdateMemoryRequest;
import com.abax.memory.domain.model.MemoryFragment;

import java.util.Optional;
import java.util.UUID;

/**
 * Core service contract for MemoryFragment CRUD operations — v2.0.0.
 * <p>
 * Provides both domain-model and DTO-based method signatures.
 * The DTO-based methods (suffixed with {@code V2}) are the primary
 * entry points for the REST API v2 layer. Domain-model methods are
 * retained for internal service-to-service calls.
 * </p>
 */
public interface MemoryService {

    // ── Domain-model methods (internal service-to-service) ──────────

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

    // ── REST v2 DTO-based methods ───────────────────────────────────

    /**
     * Creates a new memory fragment from an API v2 request.
     *
     * @param request  validated creation payload
     * @param tenantId tenant scope identifier
     * @return full response DTO
     */
    MemoryResponse createV2(CreateMemoryRequest request, String tenantId);

    /**
     * Retrieves a memory fragment by ID, scoped to the given tenant.
     *
     * @param id       fragment UUID
     * @param tenantId tenant scope identifier
     * @return full response DTO
     * @throws jakarta.ws.rs.NotFoundException if not found or cross-tenant
     */
    MemoryResponse getByIdV2(UUID id, String tenantId);

    /**
     * Partially updates a memory fragment.
     *
     * @param id       fragment UUID
     * @param request  fields to update (only non-null fields are applied)
     * @param tenantId tenant scope identifier
     * @return updated response DTO
     * @throws jakarta.ws.rs.NotFoundException if not found or cross-tenant
     * @throws IllegalArgumentException if lifecycle transition is invalid
     */
    MemoryResponse updateV2(UUID id, UpdateMemoryRequest request, String tenantId);

    /**
     * Soft-deletes a memory fragment.
     *
     * @param id       fragment UUID
     * @param tenantId tenant scope identifier
     * @throws jakarta.ws.rs.NotFoundException if not found or cross-tenant
     */
    void softDeleteV2(UUID id, String tenantId);

    /**
     * Lists memory fragments with filters and pagination.
     *
     * @param request  filter and pagination parameters
     * @param tenantId tenant scope identifier
     * @return paginated search results with facets
     */
    SearchResponse listV2(SearchRequest request, String tenantId);
}
