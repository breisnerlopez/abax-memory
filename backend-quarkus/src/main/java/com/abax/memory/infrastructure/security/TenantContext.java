package com.abax.memory.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Extracts and holds the tenant identifier for the current request.
 *
 * <h3>Tenant Resolution Strategy</h3>
 * <p>In the MVP, the tenant ID is extracted from the {@code X-Tenant-Id}
 * HTTP header. This is a deliberate simplification until OIDC-based
 * tenant resolution is fully implemented.</p>
 *
 * <p>All database queries MUST filter by the tenant ID obtained via
 * {@link #getCurrentTenantId()} to guarantee cross-tenant isolation.</p>
 *
 * <p>Cross-tenant access is enforced by returning 404 (not 403) when a
 * resource does not belong to the current tenant, so as not to reveal
 * the existence of another tenant's data.</p>
 *
 * <p><b>Security:</b> This implementation MUST be replaced before
 * production deployment. See §6.3 of the architecture document.</p>
 *
 * <p>References: Architecture document §6.3, BR-004, SC-03</p>
 */
// MOCK: Accepts X-Tenant-Id header directly without OIDC validation.
// In production, tenant_id will be extracted from the JWT claim
// issued by Keycloak after OIDC authentication.
// REPLACE_BEFORE_PROD
@RequestScoped
public class TenantContext {

    /**
     * Default tenant ID used when no header is present.
     * Only for local development convenience.
     */
    // MOCK: Hardcoded default tenant for local development without OIDC.
    // REPLACE_BEFORE_PROD
    static final String DEFAULT_TENANT_ID = "default-tenant";

    private String tenantId;

    /**
     * Returns the tenant ID for the current request scope.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If previously resolved in this scope, return cached value.</li>
     *   <li>Otherwise, extract from {@code X-Tenant-Id} header.</li>
     *   <li>If header absent, fall back to {@link #DEFAULT_TENANT_ID}.</li>
     * </ol>
     * </p>
     */
    public String getCurrentTenantId() {
        if (tenantId != null) {
            return tenantId;
        }
        // Resolve lazily — set by resource via resolveFromHeader
        tenantId = DEFAULT_TENANT_ID;
        return tenantId;
    }

    /**
     * Resolves the tenant ID from an explicit header value.
     * Called by the REST resource layer when the X-Tenant-Id header
     * is present.
     */
    public void resolveFromHeader(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            this.tenantId = headerValue.trim();
        } else {
            this.tenantId = DEFAULT_TENANT_ID;
        }
    }

    /**
     * For testing: allows explicitly setting the tenant ID.
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
