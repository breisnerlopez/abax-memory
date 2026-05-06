package com.abax.memory.domain.service;

import com.abax.memory.domain.model.DeleteNamespaceResult;

/**
 * Service contract for namespace-level administrative operations — v2.1.0.
 *
 * <p>Handles atomic deletion of all resources (memories, relations,
 * Qdrant points) belonging to a namespace within a tenant.</p>
 *
 * <p>References: ADR-013, FT-V21-004.3</p>
 */
public interface NamespaceService {

    /**
     * Atomically deletes a namespace and all its resources.
     *
     * @param namespace namespace name to delete
     * @param tenantId  tenant scope identifier
     * @return result with counts of deleted resources
     * @throws jakarta.ws.rs.NotFoundException if namespace does not exist
     */
    DeleteNamespaceResult deleteNamespace(String namespace, String tenantId);
}
