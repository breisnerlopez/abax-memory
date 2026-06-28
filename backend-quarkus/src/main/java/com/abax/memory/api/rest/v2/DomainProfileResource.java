package com.abax.memory.api.rest.v2;

import com.abax.memory.api.dto.v2.DomainProfileRequest;
import com.abax.memory.api.dto.v2.DomainProfileResponse;
import com.abax.memory.api.dto.v2.ErrorResponse;
import com.abax.memory.domain.model.DomainProfile;
import com.abax.memory.domain.service.DomainProfileService;
import com.abax.memory.infrastructure.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource implementing the Domain Profile management endpoints
 * of API v2 — CP-V21-041+.
 *
 * <h3>Base Path</h3>
 * <pre>{@code /api/v2/domains}</pre>
 *
 * <h3>Endpoints</h3>
 * <pre>
 * GET    /api/v2/domains           — list all domain profiles
 * GET    /api/v2/domains/{name}    — get a single profile by name
 * PUT    /api/v2/domains/{name}    — create or update a profile
 * DELETE /api/v2/domains/{name}    — delete a profile (admin only)
 * </pre>
 *
 * <h3>Security</h3>
 * DELETE requires the {@code X-Role: admin} header (MOCK — will be
 * replaced with OIDC role validation before production).
 *
 * <p>New in v2.1.0 — CP-V21-041+ (Gap 2: Domain profile management API).</p>
 *
 * <p>References: CP-V21-041+, EP-002, §3 of functional spec</p>
 */
@Path("/api/v2/domains")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Domains V2", description = "Domain profile management operations — API v2")
@ApplicationScoped
public class DomainProfileResource {

    private static final Logger LOG = Logger.getLogger(DomainProfileResource.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    DomainProfileService domainProfileService;

    @Inject
    TenantContext tenantContext;

    @Context
    UriInfo uriInfo;

    /**
     * Returns the current request path for error responses.
     */
    private String requestPath() {
        return uriInfo != null ? uriInfo.getRequestUri().getPath() : "/api/v2/domains";
    }

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
     * Lists all domain profiles.
     *
     * <p>Returns both active and inactive profiles. Use the
     * {@code ?active=true} query parameter to filter.</p>
     */
    @GET
    @Operation(summary = "List domain profiles", description = "Returns all domain profiles. Use ?active=true to filter active-only.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profiles listed",
                    content = @Content(schema = @Schema(implementation = DomainProfileResponse.class))),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public List<DomainProfileResponse> list(
            @HeaderParam("X-Tenant-Id") String xTenantId) {
        String tenantId = resolveTenant(xTenantId);
        LOG.debugv("Listing domain profiles for tenant: {0}", tenantId);

        List<DomainProfile> profiles = domainProfileService.listAll();
        return profiles.stream()
                .map(DomainProfileResponse::from)
                .toList();
    }

    /**
     * Retrieves a domain profile by its unique name.
     */
    @GET
    @Path("/{name}")
    @Operation(summary = "Get domain profile by name", description = "Retrieves a single domain profile by its unique name.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile found",
                    content = @Content(schema = @Schema(implementation = DomainProfileResponse.class))),
            @APIResponse(responseCode = "404", description = "Profile not found"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public DomainProfileResponse getByName(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Profile name", required = true, example = "ops")
            @PathParam("name") String name) {
        String tenantId = resolveTenant(xTenantId);
        LOG.debugv("Getting domain profile: name={0}, tenant={1}", name, tenantId);

        DomainProfile profile = domainProfileService.getByName(name);
        if (profile == null) {
            throw new jakarta.ws.rs.NotFoundException("Domain profile not found: " + name);
        }
        return DomainProfileResponse.from(profile);
    }

    /**
     * Creates or updates a domain profile.
     *
     * <p>If a profile with the given name already exists, it is fully
     * replaced (upsert semantics). Otherwise, a new profile is created.</p>
     */
    @PUT
    @Path("/{name}")
    @Operation(summary = "Create or update a domain profile", description = "Creates a new domain profile or fully replaces an existing one with the same name (upsert).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile created or updated successfully",
                    content = @Content(schema = @Schema(implementation = DomainProfileResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "403", description = "Forbidden")
    })
    public Response createOrUpdate(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @Parameter(description = "Profile name", required = true, example = "ops")
            @PathParam("name") String name,
            @Valid DomainProfileRequest request) {
        String tenantId = resolveTenant(xTenantId);
        String actorId = resolveActorId();

        // Serialize config map to JSON string
        String configJson;
        try {
            configJson = MAPPER.writeValueAsString(request.config());
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of("INVALID_CONFIG",
                            "Failed to serialize config to JSON: " + e.getMessage(),
                            requestPath()))
                    .build();
        }

        DomainProfile profile = domainProfileService.createOrUpdate(
                name, configJson, request.description(),
                request.version(), request.active(), actorId);

        boolean isNew = profile.getCreatedAt().equals(profile.getUpdatedAt());
        DomainProfileResponse response = DomainProfileResponse.from(profile);

        if (isNew) {
            return Response.ok(response)
                    .header("X-Created", "true")
                    .build();
        }
        return Response.ok(response).build();
    }

    /**
     * Deletes a domain profile (admin only).
     *
     * <p>Requires {@code X-Role: admin} header. This is a hard delete — the
     * profile is permanently removed from the database.</p>
     */
    @DELETE
    @Path("/{name}")
    @Operation(summary = "Delete a domain profile", description = "Permanently deletes a domain profile by name. Requires admin role (X-Role: admin header).")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Profile deleted"),
            @APIResponse(responseCode = "403", description = "Forbidden — admin role required"),
            @APIResponse(responseCode = "404", description = "Profile not found")
    })
    public Response delete(
            @HeaderParam("X-Tenant-Id") String xTenantId,
            @HeaderParam("X-Role") String xRole,
            @Parameter(description = "Profile name", required = true, example = "ops")
            @PathParam("name") String name) {
        // MOCK: RBAC simulation via X-Role header.
        // REPLACE_BEFORE_PROD with OIDC role claim validation.
        if (xRole == null || !"admin".equalsIgnoreCase(xRole.trim())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ErrorResponse.of("FORBIDDEN",
                            "Admin role required for domain profile deletion",
                            requestPath()))
                    .build();
        }

        String tenantId = resolveTenant(xTenantId);
        String actorId = resolveActorId();

        domainProfileService.delete(name, actorId);
        LOG.infov("Domain profile deleted: name={0}, tenant={1}, actor={2}", name, tenantId, actorId);

        return Response.noContent().build();
    }
}
