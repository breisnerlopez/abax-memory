package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.CreateRelationRequest;
import com.abax.memory.api.dto.v2.GraphResponse;
import com.abax.memory.api.dto.v2.MemoryResponse;
import com.abax.memory.api.dto.v2.SearchResponse;
import com.abax.memory.api.dto.v2.SemanticSearchRequest;
import com.abax.memory.domain.model.Relation;
import com.abax.memory.domain.service.RelationService;
import com.abax.memory.domain.service.SearchService;
import com.abax.memory.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
 * JAX-RS resource implementing Search + Graph + Relations endpoints
 * of API v2 — EP-005.
 *
 * <h3>Paths</h3>
 * <pre>
 * /api/v2/search/semantic   — semantic (vector) search     (HU-005.1.1)
 * /api/v2/search/hybrid     — hybrid search                 (HU-005.2.1)
 * /api/v2/search/similar/{id} — find similar fragments      (HU-005.3.1)
 * /api/v2/graph/{id}        — expand relationship graph     (HU-005.7.1)
 * /api/v2/relations         — create/list relations         (HU-001.8.1)
 * /api/v2/relations/{id}    — delete relation               (HU-001.8.2)
 * /api/v2/admin/reindex     — re-index all memories (admin) (HU-005.9.1)
 * </pre>
 *
 * <p>References: EP-005, HU-005.1.1 through HU-005.9.1,
 * Architecture document §5, §7.2</p>
 */
@Path("/api/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SearchResourceV2 {

    @Inject
    SearchService searchService;

    @Inject
    RelationService relationService;

    @Inject
    TenantContext tenantContext;

    // ── Tenant resolution ───────────────────────────────────────────

    /**
     * Resolves the current tenant from the {@code X-Tenant-Id} header.
     */
    private String resolveTenant(String headerValue) {
        // MOCK: Direct header-to-tenant resolution without OIDC validation.
        // REPLACE_BEFORE_PROD with JWT claim extraction.
        tenantContext.resolveFromHeader(headerValue);
        return tenantContext.getCurrentTenantId();
    }

    // ── Search Endpoints ─────────────────────────────────────────────

    /**
     * Semantic (vector-only) search — HU-005.1.1.
     */
    @POST
    @Path("/search/semantic")
    @Tag(name = "Search V2", description = "Semantic and hybrid search operations")
    @Operation(summary = "Semantic search", description = "Performs a pure semantic (vector) search over memory fragments.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Search results",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public SearchResponse semanticSearch(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Valid SemanticSearchRequest request) {
        String tenantId = resolveTenant(xTenantId);
        return searchService.semanticSearch(request, tenantId);
    }

    /**
     * Hybrid search (vector + keyword) — HU-005.2.1.
     */
    @POST
    @Path("/search/hybrid")
    @Tag(name = "Search V2")
    @Operation(summary = "Hybrid search", description = "Performs a hybrid search combining vector similarity and keyword relevance.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Search results",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public SearchResponse hybridSearch(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Valid SemanticSearchRequest request) {
        String tenantId = resolveTenant(xTenantId);
        return searchService.hybridSearch(request, tenantId);
    }

    /**
     * Find fragments similar to a given one — HU-005.3.1.
     */
    @GET
    @Path("/search/similar/{id}")
    @Tag(name = "Search V2")
    @Operation(summary = "Find similar fragments", description = "Returns memory fragments semantically similar to the given fragment.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Similar fragments found",
                    content = @Content(schema = @Schema(implementation = MemoryResponse.class))),
            @APIResponse(responseCode = "404", description = "Fragment not found"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<MemoryResponse> findSimilar(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Memory fragment UUID", required = true)
            @PathParam("id") UUID id,
            @Parameter(description = "Max number of results (1–50)", example = "10")
            @QueryParam("limit") @DefaultValue("10") int limit) {
        String tenantId = resolveTenant(xTenantId);
        return searchService.findSimilar(id, tenantId, limit);
    }

    // ── Graph Endpoints ──────────────────────────────────────────────

    /**
     * Expand relationship graph — HU-005.7.1.
     */
    @GET
    @Path("/graph/{id}")
    @Tag(name = "Graph V2", description = "Relationship graph traversal")
    @Operation(summary = "Expand graph", description = "Expands the relationship graph around a central node up to the given depth (BFS).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Graph response",
                    content = @Content(schema = @Schema(implementation = GraphResponse.class))),
            @APIResponse(responseCode = "404", description = "Fragment not found"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public GraphResponse expandGraph(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Memory fragment UUID", required = true)
            @PathParam("id") UUID id,
            @Parameter(description = "Expansion depth (1–5)", example = "2")
            @QueryParam("depth") @DefaultValue("2") int depth) {
        String tenantId = resolveTenant(xTenantId);
        return searchService.expandGraph(id, depth, tenantId);
    }

    // ── Relations Endpoints ──────────────────────────────────────────

    /**
     * Create a relationship between two fragments — HU-001.8.1.
     */
    @POST
    @Path("/relations")
    @Tag(name = "Relations V2", description = "Memory fragment relationship operations")
    @Operation(summary = "Create a relationship", description = "Creates a directed, typed relationship between two memory fragments.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Relation created"),
            @APIResponse(responseCode = "400", description = "Validation error or self-relation"),
            @APIResponse(responseCode = "404", description = "Fragment not found"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response createRelation(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Valid CreateRelationRequest request) {
        String tenantId = resolveTenant(xTenantId);
        Relation relation = relationService.createRelation(
                request.sourceId(), request.targetId(), request.relationType(), tenantId);

        return Response.created(URI.create("/api/v2/relations/" + relation.getId()))
                .entity(Map.of(
                        "id", relation.getId().toString(),
                        "sourceId", relation.getSourceId().toString(),
                        "targetId", relation.getTargetId().toString(),
                        "relationType", relation.getType().name(),
                        "tenantId", relation.getTenantId(),
                        "createdAt", relation.getCreatedAt().toString()
                ))
                .build();
    }

    /**
     * Delete a relationship — HU-001.8.2.
     */
    @DELETE
    @Path("/relations/{id}")
    @Tag(name = "Relations V2")
    @Operation(summary = "Delete a relationship", description = "Deletes a relationship by its ID, scoped to the current tenant.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Relation deleted"),
            @APIResponse(responseCode = "404", description = "Relation not found"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response deleteRelation(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Relation UUID", required = true)
            @PathParam("id") UUID id) {
        String tenantId = resolveTenant(xTenantId);
        relationService.deleteRelation(id, tenantId);
        return Response.noContent().build();
    }

    /**
     * List relations for a memory fragment.
     */
    @GET
    @Path("/relations/{id}")
    @Tag(name = "Relations V2")
    @Operation(summary = "List relations", description = "Lists all relationships for a memory fragment, optionally filtered by direction.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of relations"),
            @APIResponse(responseCode = "404", description = "Fragment not found"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<Map<String, Object>> getRelations(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Memory fragment UUID", required = true)
            @PathParam("id") UUID id,
            @Parameter(description = "Direction: incoming, outgoing, or both", example = "both")
            @QueryParam("direction") @DefaultValue("both") String direction) {
        String tenantId = resolveTenant(xTenantId);
        List<Relation> relations = relationService.getRelations(id, direction, tenantId);

        return relations.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId().toString(),
                        "sourceId", r.getSourceId().toString(),
                        "targetId", r.getTargetId().toString(),
                        "relationType", r.getType().name(),
                        "tenantId", r.getTenantId(),
                        "createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
                ))
                .toList();
    }

    // ── Admin Endpoints ──────────────────────────────────────────────

    /**
     * Re-index all active memories — HU-005.9.1 (admin only).
     */
    @POST
    @Path("/admin/reindex")
    @Tag(name = "Admin V2", description = "Administrative operations")
    @Operation(summary = "Re-index all memories", description = "Re-indexes all active (non-deleted) memory fragments for the current tenant. Requires admin role.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Re-index completed"),
            @APIResponse(responseCode = "403", description = "Forbidden — not an admin"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response reindex(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @HeaderParam("X-Role") String xRole) {
        // MOCK: RBAC simulation via X-Role header.
        // REPLACE_BEFORE_PROD with OIDC role claim validation.
        if (xRole == null || !"admin".equalsIgnoreCase(xRole.trim())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                            "errorCode", "FORBIDDEN",
                            "message", "Admin role required for re-index operation"
                    ))
                    .build();
        }

        String tenantId = resolveTenant(xTenantId);
        int indexed = searchService.reindexAll(tenantId);

        return Response.ok(Map.of(
                "status", "completed",
                "indexedFragments", indexed,
                "tenantId", tenantId
        )).build();
    }
}
