package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.CreateMemoryRequest;
import com.abax.memory.api.dto.v2.ExtractRequest;
import com.abax.memory.api.dto.v2.ExtractResponse;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.ReviewRequest;
import com.abax.memory.api.dto.v2.SearchRequest;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.UpdateMemoryRequest;
import com.abax.memory.domain.model.AuditRecord;
import com.abax.memory.domain.model.ExtractedEntity;
import com.abax.memory.domain.service.AuditService;
import com.abax.memory.domain.service.MemoryService;
import com.abax.memory.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JAX-RS resource implementing the Memory CRUD endpoints of API v2.
 *
 * <h3>Base Path</h3>
 * <pre>{@code /api/v2/memories}</pre>
 *
 * <h3>Tenant Isolation</h3>
 * All operations are scoped to the tenant extracted from the
 * {@code X-Tenant-Id} header. Cross-tenant access is denied with
 * an HTTP 404 response to avoid revealing the existence of resources
 * belonging to other tenants.
 *
 * <h3>Authentication</h3>
 * In the MVP, authentication is simulated by extracting the tenant
 * identity from the {@code X-Tenant-Id} header. This will be replaced
 * with full OIDC JWT validation before production.
 *
 * <p>References:
 *   HU-004.1.1 (Create), HU-004.2.1 (Get by ID),
 *   HU-004.3.1 (Update), HU-004.4.1 (Soft-delete),
 *   HU-004.5.1 (List with filters),
 *   Architecture document §7.2</p>
 */
@Path("/api/v2/memories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Memories V2", description = "MemoryFragment CRUD operations — API v2")
public class MemoryResourceV2 {

    @Inject
    MemoryService memoryService;

    @Inject
    AuditService auditService;

    @Inject
    TenantContext tenantContext;

    // ── Tenant resolution ───────────────────────────────────────────

    /**
     * Resolves the current tenant from the {@code X-Tenant-Id} header.
     *
     * <p>MOCK: Accepts header directly without OIDC validation.
     * REPLACE_BEFORE_PROD with JWT claim extraction.</p>
     */
    private String resolveTenant(String headerValue) {
        // MOCK: Direct header-to-tenant resolution without OIDC validation.
        // In production, this will extract tenant_id from the JWT claim.
        // REPLACE_BEFORE_PROD
        tenantContext.resolveFromHeader(headerValue);
        return tenantContext.getCurrentTenantId();
    }

    /**
     * Resolves the current actor identity.
     *
     * <p>MOCK: Uses tenant ID as actor — no OIDC user identity available.
     * REPLACE_BEFORE_PROD with JWT preferred_username or sub claim.</p>
     */
    // MOCK: tenant-as-actor identity resolution.
    // REPLACE_BEFORE_PROD
    private String resolveActorId() {
        return tenantContext.getCurrentTenantId();
    }

    // ── Endpoints ───────────────────────────────────────────────────

    /**
     * Creates a new memory fragment (HU-004.1.1).
     *
     * @param xTenantId tenant scope (mock auth header)
     * @param request   validated creation payload
     * @return 201 Created with the full MemoryResponse, plus Location header
     */
    @POST
    @Operation(summary = "Create a new memory fragment", description = "Creates a new memory fragment with the given payload. Returns the created resource.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Memory created successfully",
                    content = @Content(schema = @Schema(implementation = MemoryResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response create(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Valid CreateMemoryRequest request) {

        String tenantId = resolveTenant(xTenantId);
        MemoryResponse response = memoryService.createV2(request, tenantId);

        return Response.created(URI.create("/api/v2/memories/" + response.id()))
                .entity(response)
                .build();
    }

    /**
     * Retrieves a memory fragment by its unique identifier (HU-004.2.1).
     *
     * @param xTenantId tenant scope
     * @param id        fragment UUID (path parameter)
     * @return 200 OK with the full MemoryResponse
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get memory fragment by ID", description = "Retrieves the full detail of a memory fragment, scoped to the current tenant.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Memory found",
                    content = @Content(schema = @Schema(implementation = MemoryResponse.class))),
            @APIResponse(responseCode = "404", description = "Memory not found or cross-tenant access"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MemoryResponse getById(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Memory fragment UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathParam("id") UUID id) {

        String tenantId = resolveTenant(xTenantId);
        return memoryService.getByIdV2(id, tenantId);
    }

    /**
     * Partially updates a memory fragment (HU-004.3.1).
     *
     * <p>Only non-null fields in the request body are applied.
     * Lifecycle transitions are validated against the state machine.</p>
     *
     * @param xTenantId tenant scope
     * @param id        fragment UUID
     * @param request   fields to update
     * @return 200 OK with the updated MemoryResponse
     */
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a memory fragment", description = "Partially updates a memory fragment. Only non-null fields are applied. Lifecycle transitions are validated.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Memory updated successfully",
                    content = @Content(schema = @Schema(implementation = MemoryResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error or invalid lifecycle transition"),
            @APIResponse(responseCode = "404", description = "Memory not found or cross-tenant access"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public MemoryResponse update(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @PathParam("id") UUID id,
            @Valid UpdateMemoryRequest request) {

        String tenantId = resolveTenant(xTenantId);
        return memoryService.updateV2(id, request, tenantId);
    }

    /**
     * Soft-deletes a memory fragment (HU-004.4.1).
     *
     * <p>Sets {@code deletedAt} and transitions lifecycle to {@code DELETED}.
     * The operation is idempotent: deleting an already-deleted resource
     * returns 204.</p>
     *
     * @param xTenantId tenant scope
     * @param id        fragment UUID
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft-delete a memory fragment", description = "Marks a memory fragment as deleted. The record is preserved for audit purposes but excluded from standard queries.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Memory soft-deleted successfully"),
            @APIResponse(responseCode = "404", description = "Memory not found or cross-tenant access"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response softDelete(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @PathParam("id") UUID id) {

        String tenantId = resolveTenant(xTenantId);
        memoryService.softDeleteV2(id, tenantId);
        return Response.noContent().build();
    }

    // ── Review Workflow (EP-006 / UAT-S05) ───────────────────────────

    /**
     * Review workflow endpoint (UAT-S05).
     *
     * <p>Dispatches to the appropriate review method based on the action:
     * <ul>
     *   <li>{@code REQUEST} / {@code SUBMIT} — submit for review: DRAFT → PENDING</li>
     *   <li>{@code APPROVE} — approve and activate: PENDING → ACTIVE</li>
     *   <li>{@code REJECT}  — send back for rework: PENDING → DRAFT</li>
     * </ul>
     */
    @PUT
    @Path("/{id}/review")
    @Operation(summary = "Review a memory fragment", description = "Performs a review action on a memory fragment: request review, approve, or reject.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Review action completed successfully",
                    content = @Content(schema = @Schema(implementation = MemoryResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error or invalid lifecycle transition"),
            @APIResponse(responseCode = "404", description = "Memory not found or cross-tenant access"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response reviewMemory(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Memory fragment UUID", required = true)
            @PathParam("id") UUID id,
            @Valid ReviewRequest request) {

        String tenantId = resolveTenant(xTenantId);
        String actorId = resolveActorId();

        MemoryResponse response = switch (request.action()) {
            case REQUEST, SUBMIT -> memoryService.requestReview(id, tenantId, actorId);
            case APPROVE         -> memoryService.approveReview(id, tenantId, actorId, request.comment());
            case REJECT          -> memoryService.returnToDraft(id, tenantId, actorId, request.comment());
        };

        return Response.ok(response).build();
    }

    // ── Audit Trail (UAT-S06) ────────────────────────────────────────

    /**
     * Retrieves the audit trail for a memory fragment (UAT-S06).
     *
     * <p>Returns audit records ordered by {@code created_at} descending,
     * scoped to the current tenant.</p>
     */
    @GET
    @Path("/{id}/audit")
    @Operation(summary = "Get audit trail", description = "Returns the full audit trail for a memory fragment, ordered by timestamp descending.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Audit trail retrieved"),
            @APIResponse(responseCode = "404", description = "Memory not found or cross-tenant access"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<AuditRecord> getAuditTrail(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Memory fragment UUID", required = true)
            @PathParam("id") UUID id) {

        String tenantId = resolveTenant(xTenantId);
        // Verify the memory exists and belongs to the tenant
        memoryService.getByIdV2(id, tenantId); // throws 404 if not found / cross-tenant
        return auditService.findByMemoryId(id);
    }

    // ── Entity Extraction (UAT-S08) ──────────────────────────────────

    /**
     * Extracts named entities from text content (UAT-S08).
     *
     * <p>Does NOT persist anything — only analyzes and returns entities.
     * Uses OpenAI gpt-4o-mini exclusively (no mock degradation).</p>
     *
     * <p>New in v2.1.0: returns {@code source} and {@code extractionTimeMs}
     * metadata. Error codes: 400 (validation), 502 (provider error),
     * 503 (unavailable), 504 (timeout).</p>
     */
    @POST
    @Path("/extract")
    @Operation(summary = "Extract entities", description = "Extracts named entities from the provided text content using OpenAI gpt-4o-mini.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Entities extracted successfully",
                    content = @Content(schema = @Schema(implementation = ExtractResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error — content is required"),
            @APIResponse(responseCode = "403", description = "Forbidden"),
            @APIResponse(responseCode = "502", description = "LLM provider error"),
            @APIResponse(responseCode = "503", description = "LLM service not configured"),
            @APIResponse(responseCode = "504", description = "Extraction timed out")
    })
    public ExtractResponse extractEntities(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Valid ExtractRequest request) {

        long startTime = System.currentTimeMillis();
        String tenantId = resolveTenant(xTenantId);
        List<ExtractedEntity> entities = memoryService.extractEntities(request.getContent(), tenantId);

        List<ExtractResponse.ExtractedEntityDto> dtos = entities.stream()
                .map(e -> new ExtractResponse.ExtractedEntityDto(e.name(), e.type(), e.confidence()))
                .toList();

        long extractionTimeMs = System.currentTimeMillis() - startTime;
        return new ExtractResponse(dtos, "openai-gpt-4o-mini", extractionTimeMs);
    }

    /**
     * Lists memory fragments with filters and pagination (HU-004.5.1).
     *
     * <p>Uses {@code @BeanParam} to bind query parameters to the
     * {@link SearchRequest} record. Supports filtering by kind,
     * lifecycle state, sensitivity, scope, date range, and free-text
     * search on title and content.</p>
     *
     * @param xTenantId tenant scope
     * @param request   filter and pagination parameters
     * @return 200 OK with paginated results and facets
     */
    @GET
    @Operation(summary = "List memory fragments with filters", description = "Returns a paginated, filtered list of memory fragments for the current tenant. Includes facet aggregations.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Search results",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public SearchResponse list(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @HeaderParam("X-Role") String xRole,
            @Valid @BeanParam SearchRequest request) {

        String tenantId = resolveTenant(xTenantId);
        return memoryService.listV2(request, tenantId, xRole);
    }
}
