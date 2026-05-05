package com.abax.memory.infrastructure.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


/**
 * Jakarta REST request filter that intercepts every request to
 * {@code /api/v2/*} and extracts the tenant identifier.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li><b>OIDC Active:</b> Extracts tenant from JWT claim
 *       ({@code tenant_id}, {@code azp}, or {@code sub}) via
 *       {@link SecurityIdentity}. This is the production path.</li>
 *   <li><b>OIDC Inactive (dev/test):</b> Reads tenant from
 *       {@code X-Tenant-Id} header with a clear WARNING in the logs.</li>
 *   <li>If neither source yields a tenant, responds with HTTP 401.</li>
 * </ul>
 *
 * <p>References: EP-003 (A4), Architecture document §6.3, BR-004, SC-03</p>
 */
@Provider
public class TenantFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TenantFilter.class);

    /** Header name used for tenant identification in dev/test mode (OIDC-disabled fallback). */
    static final String TENANT_HEADER = "X-Tenant-Id";

    /** Path prefix this filter applies to. */
    static final String API_V2_PREFIX = "/api/v2";

    private final TenantContext tenantContext;

    @Inject
    SecurityIdentity securityIdentity;

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
    boolean oidcEnabled;

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

        // ── OIDC Path: extract tenant from JWT ──
        if (oidcEnabled && securityIdentity != null && !securityIdentity.isAnonymous()) {
            // TenantContext.getCurrentTenantId() will resolve from JWT claims
            String jwtTenant = tenantContext.getCurrentTenantId();
            if (jwtTenant != null && !TenantContext.DEFAULT_TENANT_ID.equals(jwtTenant)) {
                LOG.debugv("Tenant resolved via OIDC JWT: {0}", jwtTenant);
                return; // tenant already set in TenantContext
            }
        }

        // ── Dev/Test Fallback: extract tenant from X-Tenant-Id header ──
        var headerValue = requestContext.getHeaderString(TENANT_HEADER);

        if (headerValue == null || headerValue.isBlank()) {
            if (oidcEnabled) {
                // OIDC is enabled but no tenant claim in JWT
                LOG.warnv("OIDC is enabled but no tenant claim found in JWT and no X-Tenant-Id header "
                        + "for request: {0} {1}", requestContext.getMethod(), uriPath);
            } else {
                LOG.warnv("OIDC is disabled and missing X-Tenant-Id header for request: {0} {1}",
                        requestContext.getMethod(), uriPath);
            }
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("""
                                    {"errorCode":"UNAUTHORIZED","message":"Missing tenant identification. Provide OIDC token with tenant_id claim or X-Tenant-Id header."}""")
                            .build()
            );
            return;
        }

        // WARNING: OIDC is disabled — using header-based tenant resolution
        if (!oidcEnabled) {
            LOG.warnv("OIDC is DISABLED — resolving tenant from X-Tenant-Id header. "
                    + "This is acceptable ONLY in local development. "
                    + "REPLACE_BEFORE_PROD: enable OIDC and use JWT claims.");
        }

        tenantContext.resolveFromHeader(headerValue.trim());
        LOG.debugv("Tenant resolved from X-Tenant-Id header (OIDC={0}): {1}",
                oidcEnabled, tenantContext.getCurrentTenantId());
    }
}
