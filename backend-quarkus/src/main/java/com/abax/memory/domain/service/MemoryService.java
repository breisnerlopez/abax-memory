package com.abax.memory.domain.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchRequest;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.UpdateMemoryRequest;
import com.abax.memory.domain.model.ExtractedEntity;
import com.abax.memory.domain.model.MemoryFragment;

import java.util.List;
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
 *
 * <p>New in v2.0.0: review workflow, scope validation, and profile-based
 * defaults are added for EP-002 (Profiles), EP-003 (Scoping), and
 * EP-006 (Governance).</p>
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
     * @param role     caller's role for visibility filtering (admin, reviewer, consumer)
     * @return paginated search results with facets
     */
    SearchResponse listV2(SearchRequest request, String tenantId, String role);

    // ── EP-006: Review Workflow ──────────────────────────────────────

    /**
     * Requests a review for a memory fragment (DRAFT → IN_REVIEW).
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer initiating the request
     * @return updated MemoryResponse
     * @throws jakarta.ws.rs.NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid
     */
    MemoryResponse requestReview(UUID fragmentId, String tenantId, String reviewerId);

    /**
     * Approves a review (IN_REVIEW → APPROVED).
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer
     * @param comment    review comment
     * @return updated MemoryResponse
     * @throws jakarta.ws.rs.NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid
     */
    MemoryResponse approveReview(UUID fragmentId, String tenantId, String reviewerId, String comment);

    /**
     * Rejects a review (IN_REVIEW → DRAFT).
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer
     * @param comment    rejection reason (required)
     * @return updated MemoryResponse
     * @throws jakarta.ws.rs.NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid or comment missing
     */
    MemoryResponse rejectReview(UUID fragmentId, String tenantId, String reviewerId, String comment);

    /**
     * Returns a memory fragment to DRAFT for rework (PENDING → DRAFT).
     * Used by the unified review endpoint when action=REJECT.
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer
     * @param comment    rework reason (optional)
     * @return updated MemoryResponse
     * @throws jakarta.ws.rs.NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid
     */
    MemoryResponse returnToDraft(UUID fragmentId, String tenantId, String reviewerId, String comment);

    // ── EP-003: Scope Validation ────────────────────────────────────

    /**
     * Validates that a given scope identifier belongs to the specified tenant.
     *
     * @param scopeId  the scope identifier to validate
     * @param tenantId tenant scope identifier
     * @throws IllegalArgumentException if the scope is invalid for the tenant
     */
    void validateScopeBelongsToTenant(String scopeId, String tenantId);

    // ── EP-001: Entity Extraction (LLM-powered) ─────────────────────

    /**
     * Extracts named entities from raw text using the LLM service.
     * Does NOT persist anything — only analyzes and returns entities.
     *
     * @param content  the text to analyze
     * @param tenantId tenant scope identifier
     * @return list of extracted entities (empty if none found)
     */
    List<ExtractedEntity> extractEntities(String content, String tenantId);


    // ── EP-005: Entity Listing ─────────────────────────────────────

    /**
     * Lists unique extracted entities for the given tenant.
     * Entities are extracted on-the-fly from fragment content
     * and deduplicated by canonical name.
     */
    List<ExtractedEntity> listEntities(String typeFilter, String scopeId, String tenantId);
}
