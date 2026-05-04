package com.abax.memory.infrastructure.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Jakarta REST request filter that intercepts every request to
 * {@code /api/v2/*} and extracts the tenant identifier from the
 * {@code X-Tenant-Id} HTTP header.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>If the header is present and non-blank → inject into {@link TenantContext}.</li>
 *   <li>If the header is missing or blank → respond with HTTP {@code 401}.</li>
 * </ul>
 *
 * <h3>Security Notice</h3>
 * <p>In the MVP, the tenant ID is taken directly from the header without
 * OIDC validation. This is a deliberate simplification until full JWT-based
 * tenant resolution is implemented.</p>
 *
 * <p>References: EP-003 (A4), Architecture document §6.3, BR-004, SC-03</p>
 */
// MOCK: Reads X-Tenant-Id header directly. In production, tenant_id
// must be extracted from the JWT claim issued by Keycloak.
// REPLACE_BEFORE_PROD
@Provider
public class TenantFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TenantFilter.class);

    /** Header name used for tenant identification in the MVP. */
    static final String TENANT_HEADER = "X-Tenant-Id";

    /** Path prefix this filter applies to. */
    static final String API_V2_PREFIX = "/api/v2";

    private final TenantContext tenantContext;

    /**
     * Constructor-based injection. Quarkus resolves the {@link TenantContext}
     * via CDI.
     */
    public TenantFilter(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var uriPath = requestContext.getUriInfo().getPath();

        // Only apply to /api/v2/* paths
        if (!uriPath.startsWith(API_V2_PREFIX)) {
            return;
        }

        var headerValue = requestContext.getHeaderString(TENANT_HEADER);

        if (headerValue == null || headerValue.isBlank()) {
            LOG.warnv("Missing X-Tenant-Id header for request: {0} {1}",
                    requestContext.getMethod(), uriPath);
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("""
                                    {"errorCode":"UNAUTHORIZED","message":"Missing required header: X-Tenant-Id"}""")
                            .build()
            );
            return;
        }

        // MOCK: Accept header value directly.
        // REPLACE_BEFORE_PROD with JWT claim extraction.
        tenantContext.resolveFromHeader(headerValue.trim());
        LOG.debugv("Tenant resolved from header: {0}", tenantContext.getCurrentTenantId());
    }
}
