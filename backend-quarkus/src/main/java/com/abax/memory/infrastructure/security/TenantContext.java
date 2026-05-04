package com.abax.memory.infrastructure.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Extracts and holds the tenant identifier for the current request.
 *
 * <h3>Tenant Resolution Strategy</h3>
 * <p>When OIDC is configured ({@code quarkus.oidc.enabled=true}),
 * the tenant ID is extracted from the JWT claim {@code tenant_id}
 * (or {@code azp} as fallback) issued by Keycloak. This is the
 * production path.</p>
 *
 * <p>When OIDC is NOT configured (e.g., local development, test),
 * the tenant ID is read from the {@code X-Tenant-Id} HTTP header
 * with a clear WARNING in the logs.</p>
 *
 * <p>All database queries MUST filter by the tenant ID obtained via
 * {@link #getCurrentTenantId()} to guarantee cross-tenant isolation.</p>
 *
 * <p>Cross-tenant access is enforced by returning 404 (not 403) when a
 * resource does not belong to the current tenant, so as not to reveal
 * the existence of another tenant's data.</p>
 *
 * <p>References: Architecture document §6.3, BR-004, SC-03</p>
 */
@RequestScoped
public class TenantContext {

    private static final Logger LOG = Logger.getLogger(TenantContext.class);

    /**
     * Default tenant ID used when no header or JWT claim is present.
     * Only for local development convenience — never in production.
     */
    static final String DEFAULT_TENANT_ID = "default-tenant";

    /**
     * JWT claim names to inspect for tenant identification, in order of precedence.
     */
    private static final String[] TENANT_CLAIM_NAMES = {"tenant_id", "azp"};

    @Inject
    Instance<SecurityIdentity> securityIdentityInstance;

    @Inject
    Instance<JsonWebToken> jwtInstance;

    private String tenantId;

    /**
     * Returns the tenant ID for the current request scope.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If previously resolved in this scope, return cached value.</li>
     *   <li>If OIDC is active and JWT has a tenant claim, extract from JWT.</li>
     *   <li>If set via {@link #resolveFromHeader(String)}, use header value.</li>
     *   <li>Fall back to {@link #DEFAULT_TENANT_ID} with WARNING.</li>
     * </ol>
     * </p>
     */
    public String getCurrentTenantId() {
        if (tenantId != null) {
            return tenantId;
        }

        // Try to resolve from JWT (OIDC path)
        String jwtTenant = resolveFromJwt();
        if (jwtTenant != null) {
            tenantId = jwtTenant;
            LOG.debugv("Tenant resolved from JWT: {0}", tenantId);
            return tenantId;
        }

        // Fallback — must have been set by resolveFromHeader or explicitly
        tenantId = DEFAULT_TENANT_ID;
        LOG.warnv("No tenant resolved from JWT or header — using DEFAULT_TENANT_ID={0}. "
                + "This is acceptable only in local development. "
                + "REPLACE_BEFORE_PROD: ensure OIDC is enabled and JWT contains tenant_id claim.",
                DEFAULT_TENANT_ID);
        return tenantId;
    }

    /**
     * Resolves the tenant ID from an explicit header value.
     * Called by the REST resource layer when the X-Tenant-Id header
     * is present (OIDC-disabled fallback path).
     */
    public void resolveFromHeader(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            this.tenantId = headerValue.trim();
            LOG.debugv("Tenant resolved from X-Tenant-Id header: {0}", tenantId);
        } else {
            LOG.warn("Empty X-Tenant-Id header received — tenant will fall back to defaults");
            this.tenantId = null;
        }
    }

    /**
     * Attempts to extract the tenant ID from the current JWT's claims.
     * This is the production OIDC path.
     *
     * <p>Uses {@link JsonWebToken} from MicroProfile JWT (available when
     * Quarkus OIDC is enabled). Checks {@code tenant_id}, then {@code azp},
     * then {@code sub} claims.</p>
     *
     * @return the tenant identifier from JWT claims, or {@code null} if OIDC is not active
     */
    private String resolveFromJwt() {
        try {
            // Check if we have a valid SecurityIdentity
            SecurityIdentity identity = securityIdentityInstance.isResolvable()
                    ? securityIdentityInstance.get() : null;

            if (identity == null || identity.isAnonymous()) {
                LOG.debug("SecurityIdentity is anonymous or unavailable — JWT tenant resolution skipped");
                return null;
            }

            // Try to get JsonWebToken (MicroProfile JWT standard interface)
            if (jwtInstance.isResolvable()) {
                JsonWebToken jwt = jwtInstance.get();
                if (jwt != null) {
                    // Try tenant-specific claim names in order
                    for (String claimName : TENANT_CLAIM_NAMES) {
                        Object claimValue = jwt.getClaim(claimName);
                        if (claimValue instanceof String s && !s.isBlank()) {
                            return s;
                        }
                    }
                    // Fallback: use 'sub' as tenant identifier
                    String sub = jwt.getSubject();
                    if (sub != null && !sub.isBlank()) {
                        LOG.debugv("Using JWT 'sub' claim as tenant_id: {0}", sub);
                        return sub;
                    }
                }
            }

            // With quarkus-oidc alone, the principal implements JsonWebToken natively.
            // This path covers cases where the CDI-injected jwtInstance is not resolvable
            // but the SecurityIdentity principal carries the JWT claims directly.
            var principal = identity.getPrincipal();
            if (principal instanceof JsonWebToken jwt) {
                for (String claimName : TENANT_CLAIM_NAMES) {
                    Object claimValue = jwt.getClaim(claimName);
                    if (claimValue instanceof String s && !s.isBlank()) {
                        return s;
                    }
                }
                String sub = jwt.getSubject();
                if (sub != null && !sub.isBlank()) {
                    LOG.debugv("Using JWT 'sub' claim as tenant_id (from principal): {0}", sub);
                    return sub;
                }
            }

            LOG.debug("JWT principal found but no tenant_id/azp/sub claim available");
            return null;
        } catch (Exception e) {
            LOG.debugv("JWT tenant resolution failed: {0} — falling back to header", e.getMessage());
            return null;
        }
    }

    /**
     * For testing: allows explicitly setting the tenant ID.
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
