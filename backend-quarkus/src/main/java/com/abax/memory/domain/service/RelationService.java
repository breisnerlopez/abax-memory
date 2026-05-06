package com.abax.memory.domain.service;

import com.abax.memory.api.dto.v2.PatchRelationRequest;
import com.abax.memory.api.dto.v2.UpdateRelationRequest;
import com.abax.memory.domain.enums.RelationType;
import com.abax.memory.domain.model.Relation;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for typed relationships between MemoryFragments — v2.0.0.
 *
 * <p>New in EP-005: tenant-aware methods for REST API v2
 * (createRelation, deleteRelation, getRelations).</p>
 *
 * <p>New in v2.1.0: added {@code updateRelation} and {@code patchRelation}
 * for CP-V21-024 (Relation modification API).</p>
 *
 * <p>References: EP-001, EP-005, FT-001.03, HU-001.8.1, HU-001.8.2, CP-V21-024</p>
 */
public interface RelationService {

    /** Creates a new directed relationship (domain-model). */
    Relation create(Relation relation);

    /** Deletes a relationship by its id (domain-model). */
    void delete(UUID relationId);

    /** Lists all relationships where the given memory is the source. */
    List<Relation> findBySource(UUID memoryId);

    /** Lists all relationships where the given memory is the target. */
    List<Relation> findByTarget(UUID memoryId);

    // ── EP-005: Tenant-aware REST methods ─────────────────────────

    /**
     * Creates a directed relationship between two memory fragments.
     *
     * @param sourceId UUID of the source fragment
     * @param targetId UUID of the target fragment
     * @param type     relation type
     * @param tenantId tenant scope identifier
     * @return the created relation
     * @throws IllegalArgumentException if source equals target
     * @throws NotFoundException if either fragment is not found or cross-tenant
     */
    Relation createRelation(UUID sourceId, UUID targetId, RelationType type, String tenantId);

    /**
     * Deletes a relationship by ID, scoped to the given tenant.
     *
     * @param relationId UUID of the relation to delete
     * @param tenantId   tenant scope identifier
     * @throws NotFoundException if relation not found or cross-tenant
     */
    void deleteRelation(UUID relationId, String tenantId);

    /**
     * Lists all relationships for a memory fragment.
     *
     * @param fragmentId UUID of the memory fragment
     * @param direction  "incoming", "outgoing", or "both"
     * @param tenantId   tenant scope identifier
     * @return list of relations
     */
    List<Relation> getRelations(UUID fragmentId, String direction, String tenantId);

    // ── v2.1.0: Update and patch methods (CP-V21-024) ────────────

    /**
     * Fully updates an existing relationship (replaces all fields).
     *
     * @param relationId UUID of the relation to update
     * @param request    full update payload (all fields required)
     * @param tenantId   tenant scope identifier
     * @param actorId    identity of the user performing the update
     * @return the updated relation
     * @throws NotFoundException if relation not found or cross-tenant
     */
    Relation updateRelation(UUID relationId, UpdateRelationRequest request, String tenantId, String actorId);

    /**
     * Partially updates an existing relationship (only non-null fields).
     *
     * @param relationId UUID of the relation to update
     * @param request    partial update payload (at least one non-null field)
     * @param tenantId   tenant scope identifier
     * @param actorId    identity of the user performing the update
     * @return the updated relation
     * @throws NotFoundException if relation not found or cross-tenant
     * @throws IllegalArgumentException if all fields are null
     */
    Relation patchRelation(UUID relationId, PatchRelationRequest request, String tenantId, String actorId);
}
