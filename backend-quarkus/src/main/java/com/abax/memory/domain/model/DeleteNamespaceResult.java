package com.abax.memory.domain.model;

/**
 * Result of an atomic namespace deletion operation — v2.1.0.
 *
 * <p>Contains counts of deleted resources across PostgreSQL and Qdrant.</p>
 *
 * <p>References: ADR-013, FT-V21-004.3</p>
 */
public class DeleteNamespaceResult {

    private final String namespace;
    private final String tenantId;
    private final long deletedMemories;
    private final long deletedRelations;
    private final long deletedQdrantPoints;

    public DeleteNamespaceResult(String namespace, String tenantId,
                                  long deletedMemories, long deletedRelations,
                                  long deletedQdrantPoints) {
        this.namespace = namespace;
        this.tenantId = tenantId;
        this.deletedMemories = deletedMemories;
        this.deletedRelations = deletedRelations;
        this.deletedQdrantPoints = deletedQdrantPoints;
    }

    public String getNamespace() { return namespace; }
    public String getTenantId() { return tenantId; }
    public long getDeletedMemories() { return deletedMemories; }
    public long getDeletedRelations() { return deletedRelations; }
    public long getDeletedQdrantPoints() { return deletedQdrantPoints; }
}
