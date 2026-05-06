package com.abax.memory.api.rest.v2;

import com.abax.memory.domain.model.DeleteNamespaceResult;
import com.abax.memory.domain.service.NamespaceService;
import com.abax.memory.infrastructure.security.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
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
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * JAX-RS resource for administrative operations — v2.1.0.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code DELETE /api/v2/admin/namespaces/{name}} — Delete a namespace atomically</li>
 * </ul>
 * </p>
 *
 * <p>All admin endpoints require {@code memory-admin} role.</p>
 *
 * <p>References: ADR-013, FT-V21-004.3</p>
 */
@Path("/api/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResourceV2 {

    private static final Logger LOG = Logger.getLogger(AdminResourceV2.class);

    @Inject
    NamespaceService namespaceService;

    @Inject
    TenantContext tenantContext;

    // ── Tenant resolution ───────────────────────────────────────────

    private String resolveTenant(String headerValue) {
        // MOCK: Direct header-to-tenant resolution without OIDC validation.
        // REPLACE_BEFORE_PROD with JWT claim extraction.
        tenantContext.resolveFromHeader(headerValue);
        return tenantContext.getCurrentTenantId();
    }

    // ── Namespace Endpoints ──────────────────────────────────────────

    /**
     * Delete a namespace and all its resources atomically — FT-V21-004.3.
     *
     * <p>Requires:
     * <ul>
     *   <li>Role: {@code memory-admin}</li>
     *   <li>Header: {@code X-Confirm-Delete: true}</li>
     * </ul>
     * </p>
     */
    @DELETE
    @Path("/admin/namespaces/{name}")
    @Tag(name = "Admin V2", description = "Administrative operations")
    @Operation(summary = "Delete namespace", description = "Atomically deletes all memories, relations, and Qdrant points for a namespace. Requires memory-admin role and X-Confirm-Delete header.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Namespace deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteNamespaceResult.class))),
            @APIResponse(responseCode = "400", description = "Missing X-Confirm-Delete header"),
            @APIResponse(responseCode = "403", description = "Forbidden — admin role required"),
            @APIResponse(responseCode = "404", description = "Namespace not found"),
            @APIResponse(responseCode = "500", description = "Internal server error (Qdrant cleanup failed)")
    })
    public Response deleteNamespace(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @HeaderParam("X-Role") String xRole,
            @HeaderParam("X-Confirm-Delete") String xConfirmDelete,
            @Parameter(description = "Namespace name to delete", required = true, example = "production-incidents")
            @PathParam("name") String namespace) {

        // Role check — memory-admin required
        if (xRole == null || !"memory-admin".equalsIgnoreCase(xRole.trim())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of(
                            "errorCode", "FORBIDDEN",
                            "message", "Admin role (memory-admin) required for namespace deletion"
                    ))
                    .build();
        }

        // Confirmation header required
        if (!"true".equalsIgnoreCase(xConfirmDelete)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "errorCode", "CONFIRMATION_REQUIRED",
                            "message", "X-Confirm-Delete: true header required for namespace deletion"
                    ))
                    .build();
        }

        String tenantId = resolveTenant(xTenantId);

        try {
            DeleteNamespaceResult result = namespaceService.deleteNamespace(namespace, tenantId);
            return Response.ok(result).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorv(e, "Namespace deletion failed: namespace={0}, tenant={1}", namespace, tenantId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "errorCode", "NAMESPACE_DELETE_FAILED",
                            "message", "Namespace deletion failed: " + e.getMessage()
                    ))
                    .build();
        }
    }
}
