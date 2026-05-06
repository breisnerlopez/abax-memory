package com.abax.memory.api.rest.v2;

/**
 * Administrative operations — v2.1.0.
 *
 * <p>DEF-V21-001: The {@code DELETE /api/v2/admin/namespaces/{name}} endpoint
 * has been migrated to {@link SearchResourceV2#deleteNamespace} to consolidate
 * admin endpoints in a single resource class and fix a RESTEasy Reactive path
 * resolution conflict where the endpoint was not registered at runtime.</p>
 *
 * <p>This class is kept as documentation reference. All administrative
 * endpoints are now served by {@link SearchResourceV2}.</p>
 *
 * <p>References: ADR-013, FT-V21-004.3, DEF-V21-001</p>
 */
public class AdminResourceV2 {
    // All endpoints migrated to SearchResourceV2 — see DEF-V21-001
    private AdminResourceV2() {
        // utility class — not instantiated
    }
}
