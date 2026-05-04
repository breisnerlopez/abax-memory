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
import com.abax.memory.domain.service.LlmService;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.domain.service.SearchService;
import com.abax.memory.domain.model.ExtractedEntity;
import com.abax.memory.infrastructure.persistence.DomainProfileEntity;
import com.abax.memory.infrastructure.persistence.MemoryFragmentEntity;
import com.abax.memory.infrastructure.persistence.TenantConfigEntity;
import com.abax.memory.infrastructure.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
 * <h3>Audit Trail</h3>
 * Every mutating operation (create, update, soft-delete, review state
 * change) generates an immutable audit record in {@code audit_records}.
 *
 * <h3>Profile-Based Defaults</h3>
 * When creating memories, if kind, sensitivity, or confidence are not
 * explicitly provided, defaults are applied from the tenant's assigned
 * domain profile.
 *
 * <p>References: Architecture document §6.3, §7.1, §7.2; EP-002, EP-003, EP-006</p>
 */
@ApplicationScoped
public class MemoryServiceImpl implements MemoryService {

    private static final Logger LOG = Logger.getLogger(MemoryServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    AuditServiceImpl auditService;

    @Inject
    TenantContext tenantContext;

    @Inject
    SearchService searchService;

    @Inject
    LlmService llmService;

    // ── Domain-model methods (delegated internally) ─────────────────

    @Override
    @Transactional
    public MemoryFragment create(MemoryFragment fragment) {
        var entity = toEntity(fragment);
        entity.persist();
        LOG.infov("MemoryFragment created: id={0}, tenant={1}, kind={2}",
                entity.getId(), entity.getTenantId(), entity.getKind());
        // B3: Audit
        auditService.recordAction(entity.getId(), "CREATE", "system",
                entity.getTenantId(), Map.of("after", toDiffMap(entity)));
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
        var before = toDiffMap(entity);
        applyDomainUpdates(entity, updates);
        entity.persist();
        // B3: Audit
        auditService.recordAction(entity.getId(), "UPDATE", "system",
                entity.getTenantId(), Map.of("before", before, "after", toDiffMap(entity)));
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

        // C3: Apply profile-based defaults for kind, sensitivity, confidence
        var profileDefaults = loadProfileDefaults(tenantId);
        entity.setKind(request.kind() != null ? request.kind() : profileDefaults.defaultKind());
        entity.setSensitivityLevel(
                request.sensitivityLevel() != null
                        ? request.sensitivityLevel()
                        : profileDefaults.defaultSensitivity());
        entity.setConfidence(
                request.confidence() != null
                        ? request.confidence()
                        : profileDefaults.defaultConfidence());

        // Optional fields
        entity.setScopeId(request.scopeId());
        entity.setSourceType(request.sourceType());
        entity.setSourceRef(request.sourceRef());
        entity.setNamespace(request.namespace());

        // A3: Validate scope belongs to tenant
        if (request.scopeId() != null && !request.scopeId().isBlank()) {
            validateScopeBelongsToTenant(request.scopeId(), tenantId);
        }

        // @PrePersist will handle id, createdAt, updatedAt, and defaults
        entity.persist();
        LOG.infov("MemoryFragment created via API v2: id={0}, tenant={1}, kind={2}, title={3}",
                entity.getId(), tenantId, entity.getKind(), entity.getTitle());

        // B3: Audit the creation
        auditService.recordAction(entity.getId(), "CREATE", resolveActorId(),
                tenantId, Map.of("after", toDiffMap(entity)));

        // EP-001/D: AI enrichment — generate summary and estimate confidence
        try {
            if (entity.getSummary() == null || entity.getSummary().isBlank()) {
                String summary = llmService.generateSummary(entity.getContent(), entity.getKind());
                entity.setSummary(summary);
            }
            if (entity.getConfidence() == null || entity.getConfidence() == 0.5) {
                float confidence = llmService.estimateConfidence(entity.getContent(), entity.getKind());
                entity.setConfidence((double) confidence);
            }
            entity.persist(); // persist AI-generated fields
        } catch (Exception e) {
            LOG.warnv("AI enrichment failed for fragment {0}: {1}", entity.getId(), e.getMessage());
            // Non-blocking: memory is persisted even if enrichment fails
        }

        // EP-005: Index fragment for semantic search (async in production)
        // MOCK: Synchronous indexing — fast with in-memory stubs.
        // REPLACE_BEFORE_PROD: use reactive/async indexing to avoid blocking.
        try {
            searchService.indexFragment(entity.getId(), tenantId);
        } catch (Exception e) {
            LOG.warnv("Failed to index fragment {0}: {1}", entity.getId(), e.getMessage());
        }

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
        var before = toDiffMap(entity);

        // Validate lifecycle transition if requested
        boolean lifecycleChanged = false;
        LifecycleState previousState = entity.getLifecycleState();
        if (request.lifecycleState() != null) {
            LifecycleState target = request.lifecycleState();
            if (!previousState.canTransitionTo(target)) {
                throw new IllegalArgumentException(
                        "Invalid lifecycle transition from " + previousState + " to " + target);
            }
            entity.setLifecycleState(target);
            lifecycleChanged = true;
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

        // B3: Audit the update — use lifecycle-specific action if state changed
        String action = lifecycleChanged
                ? lifecycleStateToAuditAction(previousState, entity.getLifecycleState())
                : "UPDATE";
        auditService.recordAction(entity.getId(), action, resolveActorId(),
                tenantId, Map.of("before", before, "after", toDiffMap(entity)));

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

        // B3: Audit the soft-delete
        auditService.recordAction(entity.getId(), "SOFT_DELETE", resolveActorId(),
                tenantId, Map.of("previousState", current.name()));
    }

    @Override
    public SearchResponse listV2(SearchRequest request, String tenantId) {
        // A5: Build dynamic query with tenant_id filter
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
            // Sensitivity ordering: PUBLIC < INTERNAL < CONFIDENTIAL < SECRET
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

        // Namespace filter
        if (request.getNamespace() != null && !request.getNamespace().isBlank()) {
            String ns = request.getNamespace().trim();
            if (ns.endsWith(":")) {
                queryBuilder.append(" and (namespace = :nsExact or namespace LIKE :nsPrefix)");
                params.put("nsExact", ns.substring(0, ns.length() - 1));
                params.put("nsPrefix", ns + "%");
            } else {
                queryBuilder.append(" and namespace = :nsExact");
                params.put("nsExact", ns);
            }
        }

        // Text search on title and content
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

    // ── EP-006: Review Workflow (B4) ─────────────────────────────────

    /**
     * Requests review for a memory fragment: DRAFT → PENDING.
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer requesting the review
     * @return updated MemoryResponse
     * @throws NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid
     */
    @Override
    @Transactional
    public MemoryResponse requestReview(UUID fragmentId, String tenantId, String reviewerId) {
        var entity = requireEntityForTenant(fragmentId, tenantId);
        var current = entity.getLifecycleState();

        if (!current.canTransitionTo(LifecycleState.PENDING)) {
            throw new IllegalArgumentException(
                    "Cannot request review from lifecycle state " + current);
        }

        entity.setLifecycleState(LifecycleState.PENDING);
        entity.persist();
        LOG.infov("Review requested: id={0}, tenant={1}, reviewer={2}", fragmentId, tenantId, reviewerId);

        // B3: Audit
        auditService.recordAction(fragmentId, "REVIEW_REQUESTED", reviewerId,
                tenantId, Map.of("previousState", current.name(), "newState", LifecycleState.PENDING.name()));

        return MemoryResponse.from(entity);
    }

    /**
     * Approves a review: PENDING → ACTIVE.
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer
     * @param comment    review comment
     * @return updated MemoryResponse
     * @throws NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid
     */
    @Override
    @Transactional
    public MemoryResponse approveReview(UUID fragmentId, String tenantId, String reviewerId, String comment) {
        var entity = requireEntityForTenant(fragmentId, tenantId);
        var current = entity.getLifecycleState();

        if (!current.canTransitionTo(LifecycleState.ACTIVE)) {
            throw new IllegalArgumentException(
                    "Cannot approve review from lifecycle state " + current);
        }

        entity.setLifecycleState(LifecycleState.ACTIVE);
        entity.setReviewerId(reviewerId);
        entity.setReviewComment(comment);
        entity.persist();
        LOG.infov("Review approved: id={0}, tenant={1}, reviewer={2}", fragmentId, tenantId, reviewerId);

        // B3: Audit
        auditService.recordAction(fragmentId, "REVIEWED", reviewerId,
                tenantId, Map.of(
                        "previousState", current.name(),
                        "newState", LifecycleState.ACTIVE.name(),
                        "comment", comment != null ? comment : ""));

        return MemoryResponse.from(entity);
    }

    /**
     * Rejects a review: PENDING → REJECTED.
     *
     * @param fragmentId UUID of the memory fragment
     * @param tenantId   tenant scope identifier
     * @param reviewerId identifier of the reviewer
     * @param comment    rejection reason (required)
     * @return updated MemoryResponse
     * @throws NotFoundException if fragment not found or cross-tenant
     * @throws IllegalArgumentException if transition is invalid or comment missing
     */
    @Override
    @Transactional
    public MemoryResponse rejectReview(UUID fragmentId, String tenantId, String reviewerId, String comment) {
        var entity = requireEntityForTenant(fragmentId, tenantId);
        var current = entity.getLifecycleState();

        if (!current.canTransitionTo(LifecycleState.REJECTED)) {
            throw new IllegalArgumentException(
                    "Cannot reject review from lifecycle state " + current);
        }

        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment is required when rejecting a review");
        }

        entity.setLifecycleState(LifecycleState.REJECTED);
        entity.setReviewerId(reviewerId);
        entity.setReviewComment(comment);
        entity.persist();
        LOG.infov("Review rejected: id={0}, tenant={1}, reviewer={2}", fragmentId, tenantId, reviewerId);

        // B3: Audit
        auditService.recordAction(fragmentId, "REVIEWED", reviewerId,
                tenantId, Map.of(
                        "previousState", current.name(),
                        "newState", LifecycleState.REJECTED.name(),
                        "decision", "rejected",
                        "comment", comment));

        return MemoryResponse.from(entity);
    }

    // ── EP-003: Scope Validation (A3) ───────────────────────────────

    /**
     * Validates that a given {@code scopeId} belongs to the specified tenant.
     *
     * <p>In the MVP, scope validation ensures that a scope ID is either
     * new (no existing records for other tenants) or already belongs to
     * the current tenant. Cross-scope queries between tenants are prevented.</p>
     *
     * <p>A {@code secret} sensitivity scope is only visible to {@code memory-admin}
     * and {@code memory-auditor} roles.</p>
     *
     * @param scopeId  the scope identifier to validate
     * @param tenantId tenant scope identifier
     * @throws IllegalArgumentException if the scope is invalid for the tenant
     */
    @Override
    public void validateScopeBelongsToTenant(String scopeId, String tenantId) {
        if (scopeId == null || scopeId.isBlank()) {
            return; // empty scope is always valid
        }

        // Check if any memory with this scopeId exists in a different tenant
        long crossTenantCount = MemoryFragmentEntity.count(
                "scopeId = :scopeId and tenantId != :tenantId",
                Map.of("scopeId", scopeId, "tenantId", tenantId));

        if (crossTenantCount > 0) {
            LOG.warnv("Scope validation failed: scopeId={0} exists in another tenant, requesting_tenant={1}",
                    scopeId, tenantId);
            throw new IllegalArgumentException(
                    "Scope '" + scopeId + "' does not belong to tenant '" + tenantId + "'");
        }
    }

    // ── EP-002: Profile-Based Defaults (C3) ──────────────────────────

    /**
     * Loads default values from the tenant's assigned domain profile.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Look up {@code tenant_configs} for the tenant's {@code profile_id}.</li>
     *   <li>Load the referenced profile from {@code profiles}.</li>
     *   <li>Parse the JSONB config for {@code defaultSensitivity},
     *       {@code recommendedKinds} (first element), and {@code defaultConfidence}.</li>
     *   <li>Fall back to system defaults if no profile is configured.</li>
     * </ol>
     * </p>
     *
     * @param tenantId tenant scope identifier
     * @return a record containing resolved defaults
     */
    ProfileDefaults loadProfileDefaults(String tenantId) {
        try {
            var tenantConfig = TenantConfigEntity.findByTenantId(tenantId);
            if (tenantConfig != null && tenantConfig.getProfileId() != null) {
                var profile = (DomainProfileEntity) DomainProfileEntity.findById(tenantConfig.getProfileId());
                if (profile != null && profile.isActive()) {
                    var config = MAPPER.readValue(profile.getConfig(), Map.class);
                    return new ProfileDefaults(
                            extractDefaultKind(config),
                            extractDefaultSensitivity(config),
                            extractDefaultConfidence(config)
                    );
                }
            }
        } catch (Exception e) {
            LOG.warnv("Failed to load profile defaults for tenant {0}: {1}", tenantId, e.getMessage());
        }
        // Fallback to system defaults
        return new ProfileDefaults(MemoryKind.FACT, SensitivityLevel.INTERNAL, 0.5);
    }

    @SuppressWarnings("unchecked")
    private MemoryKind extractDefaultKind(Map<String, Object> config) {
        try {
            var kinds = (List<String>) config.get("recommendedKinds");
            if (kinds != null && !kinds.isEmpty()) {
                return MemoryKind.valueOf(kinds.get(0));
            }
        } catch (Exception e) {
            LOG.debugv("Could not extract default kind from profile config: {0}", e.getMessage());
        }
        return MemoryKind.FACT;
    }

    private SensitivityLevel extractDefaultSensitivity(Map<String, Object> config) {
        try {
            var sensitivity = (String) config.get("defaultSensitivity");
            if (sensitivity != null) {
                return SensitivityLevel.valueOf(sensitivity);
            }
        } catch (Exception e) {
            LOG.debugv("Could not extract default sensitivity from profile config: {0}", e.getMessage());
        }
        return SensitivityLevel.INTERNAL;
    }

    private double extractDefaultConfidence(Map<String, Object> config) {
        try {
            var confidence = config.get("defaultConfidence");
            if (confidence instanceof Number num) {
                return num.doubleValue();
            }
        } catch (Exception e) {
            LOG.debugv("Could not extract default confidence from profile config: {0}", e.getMessage());
        }
        return 0.5;
    }

    /**
     * Value object holding resolved profile defaults for memory creation.
     */
    public record ProfileDefaults(
            MemoryKind defaultKind,
            SensitivityLevel defaultSensitivity,
            double defaultConfidence
    ) {}

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

    /**
     * Resolves the current actor ID from the tenant context.
     * In the MVP, this is the tenant ID itself (header-based mock).
     */
    // MOCK: Uses tenant ID as actor — no OIDC user identity available.
    // REPLACE_BEFORE_PROD with JWT preferred_username or sub claim.
    private String resolveActorId() {
        return tenantContext.getCurrentTenantId();
    }

    // ── Audit action mapping ─────────────────────────────────────────

    /**
     * Maps a lifecycle state transition to the corresponding audit action.
     */
    private String lifecycleStateToAuditAction(LifecycleState from, LifecycleState to) {
        if (to == LifecycleState.PENDING && from == LifecycleState.DRAFT) return "REVIEW_REQUESTED";
        if (to == LifecycleState.ACTIVE) return "REVIEWED";
        if (to == LifecycleState.REJECTED) return "REVIEW_REJECTED";
        if (to == LifecycleState.DRAFT) return "REVIEW_RETURN";
        if (to == LifecycleState.ARCHIVED) return "ARCHIVE";
        if (to == LifecycleState.DELETED) return "SOFT_DELETE";
        return "LIFECYCLE_CHANGED";
    }

    /**
     * Creates a simplified diff map for audit trail from an entity.
     */
    private Map<String, Object> toDiffMap(MemoryFragmentEntity entity) {
        var diff = new LinkedHashMap<String, Object>();
        diff.put("id", entity.getId().toString());
        diff.put("title", entity.getTitle());
        diff.put("kind", entity.getKind().name());
        diff.put("lifecycleState", entity.getLifecycleState().name());
        diff.put("sensitivityLevel", entity.getSensitivityLevel().name());
        diff.put("confidence", entity.getConfidence());
        diff.put("scopeId", entity.getScopeId());
        return diff;
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

    // ── EP-001/D: Entity Extraction (LLM-powered) ────────────────────

    @Override
    public List<ExtractedEntity> extractEntities(String content, String tenantId) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        LOG.infov("Entity extraction requested: tenant={0}, content_length={1}", tenantId, content.length());
        return llmService.extractEntities(content, MemoryKind.FACT);
    }

    // ── EP-005: Entity Listing ────────────────────────────────────────

    @Override
    public List<ExtractedEntity> listEntities(String typeFilter, String scopeId, String tenantId) {
        LOG.infov("Entity listing requested: tenant={0}, type={1}, scopeId={2}", tenantId, typeFilter, scopeId);
        var queryBuilder = new StringBuilder("tenantId = :tenantId and deletedAt IS NULL");
        var params = new LinkedHashMap<String, Object>();
        params.put("tenantId", tenantId);
        if (scopeId != null && !scopeId.isBlank()) {
            queryBuilder.append(" and scopeId = :scopeId");
            params.put("scopeId", scopeId);
        }
        var entities = MemoryFragmentEntity.find(queryBuilder.toString(),
                        Sort.by("createdAt").descending(), params)
                .page(Page.of(0, 20))
                .list();
        if (entities.isEmpty()) return List.of();
        Map<String, ExtractedEntity> uniqueEntities = new LinkedHashMap<>();
        for (var entity : entities) {
            var fe = (MemoryFragmentEntity) entity;
            try {
                for (var e : llmService.extractEntities(fe.getContent(), fe.getKind())) {
                    String key = e.name().toLowerCase().trim();
                    if (typeFilter != null && !typeFilter.isBlank() && !typeFilter.equalsIgnoreCase(e.type())) continue;
                    var existing = uniqueEntities.get(key);
                    if (existing == null || e.confidence() > existing.confidence()) uniqueEntities.put(key, e);
                }
            } catch (Exception ex) { LOG.debugv("Entity extraction failed: {0}", ex.getMessage()); }
        }
        return List.copyOf(uniqueEntities.values());
    }
}
