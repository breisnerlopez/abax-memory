package com.abax.memory.infrastructure.service;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchRequest;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.UpdateMemoryRequest;
import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;
import com.abax.memory.domain.enums.SensitivityLevel;
import com.abax.memory.domain.model.MemoryFragment;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * V2 implementation of {@link MemoryService} using Panache active-record
 * persistence with PostgreSQL.
 *
 * <h3>Tenant Isolation</h3>
 * Every method that queries the database includes a {@code tenant_id}
 * filter. If a resource is requested that does not belong to the
 * caller's tenant, a {@link NotFoundException} (404) is thrown rather
 * than a 403, so as not to reveal the existence of the resource.
 *
 * <h3>Soft-Delete</h3>
 * Deleted records are excluded from all query results by filtering
 * on {@code deletedAt IS NULL}. Administrative endpoints may override
 * this in the future.
 *
 * <p>References: Architecture document §6.3, §7.2, BR-005</p>
 */
@ApplicationScoped
public class MemoryServiceImpl implements MemoryService {

    private static final Logger LOG = Logger.getLogger(MemoryServiceImpl.class);

    // ── Domain-model methods (delegated internally) ─────────────────

    @Override
    @Transactional
    public MemoryFragment create(MemoryFragment fragment) {
        var entity = toEntity(fragment);
        entity.persist();
        LOG.infov("MemoryFragment created: id={0}, tenant={1}, kind={2}",
                entity.getId(), entity.getTenantId(), entity.getKind());
        return toDomain(entity);
    }

    @Override
    public Optional<MemoryFragment> findById(UUID id) {
        var entity = MemoryFragmentEntity.findById(id);
        return Optional.ofNullable((MemoryFragmentEntity) entity).map(this::toDomain);
    }

    @Override
    @Transactional
    public MemoryFragment update(UUID id, MemoryFragment updates) {
        var entity = requireEntity(id, updates.getTenantId());
        applyDomainUpdates(entity, updates);
        entity.persist();
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        // Domain-model softDelete requires tenantId context — not suitable
        // without explicit tenant resolution. Use softDeleteV2 instead.
        throw new UnsupportedOperationException(
                "Use softDeleteV2(id, tenantId) for tenant-scoped soft-delete");
    }

    // ── REST v2 DTO-based methods ───────────────────────────────────

    @Override
    @Transactional
    public MemoryResponse createV2(CreateMemoryRequest request, String tenantId) {
        var entity = new MemoryFragmentEntity();
        entity.setTenantId(tenantId);

        // Required fields
        entity.setTitle(request.title());
        entity.setContent(request.content());

        // Optional fields with defaults
        entity.setKind(request.kind() != null ? request.kind() : MemoryKind.KNOWLEDGE);
        entity.setScopeId(request.scopeId());
        entity.setSensitivityLevel(
                request.sensitivityLevel() != null
                        ? request.sensitivityLevel()
                        : SensitivityLevel.INTERNAL);
        entity.setSourceType(request.sourceType());
        entity.setSourceRef(request.sourceRef());
        entity.setConfidence(request.confidence() != null ? request.confidence() : 0.5);

        // @PrePersist will handle id, createdAt, updatedAt, and defaults

        entity.persist();
        LOG.infov("MemoryFragment created via API v2: id={0}, tenant={1}, kind={2}, title={3}",
                entity.getId(), tenantId, entity.getKind(), entity.getTitle());

        return MemoryResponse.from(entity);
    }

    @Override
    public MemoryResponse getByIdV2(UUID id, String tenantId) {
        var entity = requireEntityForTenant(id, tenantId);
        return MemoryResponse.from(entity);
    }

    @Override
    @Transactional
    public MemoryResponse updateV2(UUID id, UpdateMemoryRequest request, String tenantId) {
        var entity = requireEntityForTenant(id, tenantId);

        // Validate lifecycle transition if requested
        if (request.lifecycleState() != null) {
            LifecycleState current = entity.getLifecycleState();
            LifecycleState target = request.lifecycleState();
            if (!current.canTransitionTo(target)) {
                throw new IllegalArgumentException(
                        "Invalid lifecycle transition from " + current + " to " + target);
            }
            entity.setLifecycleState(target);
        }

        // Apply partial updates (only non-null fields)
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.content() != null) {
            entity.setContent(request.content());
        }
        if (request.summary() != null) {
            entity.setSummary(request.summary());
        }
        if (request.sensitivityLevel() != null) {
            entity.setSensitivityLevel(request.sensitivityLevel());
        }
        if (request.confidence() != null) {
            entity.setConfidence(request.confidence());
        }

        // @PreUpdate will update updatedAt
        entity.persist();
        LOG.infov("MemoryFragment updated via API v2: id={0}, tenant={1}", id, tenantId);

        return MemoryResponse.from(entity);
    }

    @Override
    @Transactional
    public void softDeleteV2(UUID id, String tenantId) {
        var entity = requireEntityForMutation(id, tenantId);

        // Double-check: DELETED is a terminal state
        if (entity.isDeleted()) {
            return; // idempotent
        }

        // Validate the transition
        LifecycleState current = entity.getLifecycleState();
        if (!LifecycleState.softDeletableFrom().contains(current)) {
            throw new IllegalArgumentException(
                    "Cannot soft-delete from lifecycle state " + current);
        }

        entity.softDelete();
        entity.persist();
        LOG.infov("MemoryFragment soft-deleted via API v2: id={0}, tenant={1}", id, tenantId);
    }

    @Override
    public SearchResponse listV2(SearchRequest request, String tenantId) {
        // Build dynamic query
        var queryBuilder = new StringBuilder("tenantId = :tenantId and deletedAt IS NULL");
        var params = new LinkedHashMap<String, Object>();
        params.put("tenantId", tenantId);

        // Additional filters
        if (request.getKinds() != null && !request.getKinds().isEmpty()) {
            queryBuilder.append(" and kind IN :kinds");
            params.put("kinds", request.getKinds());
        }
        if (request.getLifecycleStates() != null && !request.getLifecycleStates().isEmpty()) {
            queryBuilder.append(" and lifecycleState IN :lifecycleStates");
            params.put("lifecycleStates", request.getLifecycleStates());
        }
        if (request.getSensitivityMax() != null) {
            // Sensitivity ordering: PUBLIC < INTERNAL < CONFIDENTIAL < RESTRICTED
            queryBuilder.append(" and sensitivityLevel <= :sensitivityMax");
            params.put("sensitivityMax", request.getSensitivityMax());
        }
        if (request.getScopeIds() != null && !request.getScopeIds().isEmpty()) {
            queryBuilder.append(" and scopeId IN :scopeIds");
            params.put("scopeIds", request.getScopeIds());
        }
        if (request.getFromDate() != null) {
            queryBuilder.append(" and createdAt >= :fromDate");
            params.put("fromDate", request.getFromDate());
        }
        if (request.getToDate() != null) {
            queryBuilder.append(" and createdAt <= :toDate");
            params.put("toDate", request.getToDate());
        }

        // Text search on title and content (simple LIKE, Qdrant search is POST /search)
        if (request.getQuery() != null && !request.getQuery().isBlank()
                && !"*".equals(request.getQuery().trim())) {
            queryBuilder.append(" and (title LIKE :queryText or content LIKE :queryText)");
            params.put("queryText", "%" + request.getQuery().trim() + "%");
        }

        String fullQuery = queryBuilder.toString();

        // Count
        long total = MemoryFragmentEntity.count(fullQuery, params);

        // Paginated query
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, Math.min(100, request.getSize()));

        var entities = MemoryFragmentEntity.find(fullQuery, Sort.by("createdAt").descending(), params)
                .page(Page.of(page, size))
                .list();

        List<MemoryResponse> items = entities.stream()
                .map(e -> MemoryResponse.from((MemoryFragmentEntity) e))
                .toList();

        // Build facets
        Map<String, Map<String, Long>> facets = buildFacets(tenantId);

        return new SearchResponse(items, total, page, size, facets);
    }

    // ── Private helpers ─────────────────────────────────────────────

    /**
     * Retrieves an entity by ID and validates it belongs to the given tenant.
     * Also excludes soft-deleted records from standard queries.
     * Throws 404 if not found, cross-tenant, or soft-deleted.
     */
    private MemoryFragmentEntity requireEntityForTenant(UUID id, String tenantId) {
        var entity = (MemoryFragmentEntity) MemoryFragmentEntity.findById(id);
        if (entity == null || entity.isDeleted()) {
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            // Cross-tenant access: return 404 to avoid revealing existence
            LOG.warnv("Cross-tenant access attempt: id={0}, requesting_tenant={1}, resource_tenant={2}",
                    id, tenantId, entity.getTenantId());
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        return entity;
    }

    /**
     * Retrieves an entity by ID for mutation operations (including soft-delete).
     * Allows access to soft-deleted records for idempotent delete.
     * Still validates tenant membership.
     */
    private MemoryFragmentEntity requireEntityForMutation(UUID id, String tenantId) {
        var entity = (MemoryFragmentEntity) MemoryFragmentEntity.findById(id);
        if (entity == null) {
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            // Cross-tenant access: return 404 to avoid revealing existence
            LOG.warnv("Cross-tenant access attempt: id={0}, requesting_tenant={1}, resource_tenant={2}",
                    id, tenantId, entity.getTenantId());
            throw new NotFoundException("Memory fragment not found: " + id);
        }
        return entity;
    }

    /**
     * Retrieves an entity by ID with tenant validation.
     * Variant used by internal domain-model methods.
     */
    private MemoryFragmentEntity requireEntity(UUID id, String tenantId) {
        return requireEntityForTenant(id, tenantId);
    }

    // ── Entity ↔ Domain mapping ─────────────────────────────────────

    private MemoryFragmentEntity toEntity(MemoryFragment domain) {
        var entity = new MemoryFragmentEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setScopeId(domain.getScopeId());
        entity.setKind(domain.getKind());
        entity.setTitle(domain.getTitle());
        entity.setContent(domain.getContent());
        entity.setSummary(domain.getSummary());
        entity.setLifecycleState(domain.getLifecycleState());
        entity.setSensitivityLevel(domain.getSensitivityLevel());
        entity.setSourceType(domain.getSourceType());
        entity.setSourceRef(domain.getSourceRef());
        entity.setConfidence(domain.getConfidence());
        entity.setEmbeddingId(domain.getEmbeddingId());
        entity.setReviewerId(domain.getReviewerId());
        entity.setReviewComment(domain.getReviewComment());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        return entity;
    }

    private MemoryFragment toDomain(MemoryFragmentEntity entity) {
        var domain = new MemoryFragment();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setScopeId(entity.getScopeId());
        domain.setKind(entity.getKind());
        domain.setTitle(entity.getTitle());
        domain.setContent(entity.getContent());
        domain.setSummary(entity.getSummary());
        domain.setLifecycleState(entity.getLifecycleState());
        domain.setSensitivityLevel(entity.getSensitivityLevel());
        domain.setSourceType(entity.getSourceType());
        domain.setSourceRef(entity.getSourceRef());
        domain.setConfidence(entity.getConfidence());
        domain.setEmbeddingId(entity.getEmbeddingId());
        domain.setReviewerId(entity.getReviewerId());
        domain.setReviewComment(entity.getReviewComment());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setDeletedAt(entity.getDeletedAt());
        return domain;
    }

    private void applyDomainUpdates(MemoryFragmentEntity entity, MemoryFragment updates) {
        if (updates.getTitle() != null) entity.setTitle(updates.getTitle());
        if (updates.getContent() != null) entity.setContent(updates.getContent());
        if (updates.getSummary() != null) entity.setSummary(updates.getSummary());
        if (updates.getLifecycleState() != null) entity.setLifecycleState(updates.getLifecycleState());
        if (updates.getSensitivityLevel() != null) entity.setSensitivityLevel(updates.getSensitivityLevel());
        if (updates.getConfidence() != null) entity.setConfidence(updates.getConfidence());
        if (updates.getSourceType() != null) entity.setSourceType(updates.getSourceType());
        if (updates.getSourceRef() != null) entity.setSourceRef(updates.getSourceRef());
        if (updates.getReviewerId() != null) entity.setReviewerId(updates.getReviewerId());
        if (updates.getReviewComment() != null) entity.setReviewComment(updates.getReviewComment());
    }

    /**
     * Builds facet aggregations for kind, lifecycle state, and sensitivity level.
     * These drive the UI filter controls for the list/search view.
     */
    private Map<String, Map<String, Long>> buildFacets(String tenantId) {
        Map<String, Map<String, Long>> facets = new LinkedHashMap<>();

        // Kind facet
        Map<String, Long> kindCounts = new LinkedHashMap<>();
        for (MemoryKind kind : MemoryKind.values()) {
            long count = MemoryFragmentEntity.count(
                    "tenantId = :tenantId and kind = :kind and deletedAt IS NULL",
                    Map.of("tenantId", tenantId, "kind", kind));
            if (count > 0) {
                kindCounts.put(kind.name(), count);
            }
        }
        facets.put("kind", kindCounts);

        // Lifecycle state facet
        Map<String, Long> stateCounts = new LinkedHashMap<>();
        for (LifecycleState state : LifecycleState.values()) {
            if (state == LifecycleState.DELETED) continue; // excluded by deletedAt filter
            long count = MemoryFragmentEntity.count(
                    "tenantId = :tenantId and lifecycleState = :state and deletedAt IS NULL",
                    Map.of("tenantId", tenantId, "state", state));
            if (count > 0) {
                stateCounts.put(state.name(), count);
            }
        }
        facets.put("lifecycleState", stateCounts);

        // Sensitivity level facet
        Map<String, Long> sensitivityCounts = new LinkedHashMap<>();
        for (SensitivityLevel level : SensitivityLevel.values()) {
            long count = MemoryFragmentEntity.count(
                    "tenantId = :tenantId and sensitivityLevel = :level and deletedAt IS NULL",
                    Map.of("tenantId", tenantId, "level", level));
            if (count > 0) {
                sensitivityCounts.put(level.name(), count);
            }
        }
        facets.put("sensitivityLevel", sensitivityCounts);

        return facets;
    }
}
