package com.abax.memory.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to {@code audit_records} — append-only, immutable
 * audit log of every mutation in the system.
 *
 * <p>No update or delete operations are permitted on audit records.
 * The table is append-only by design.</p>
 *
 * <p>References: EP-006, Flyway V4, Architecture document §7.1</p>
 */
@Entity
@Table(name = "audit_records", indexes = {
        @Index(name = "idx_audit_memory_id", columnList = "memory_id"),
        @Index(name = "idx_audit_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_audit_created_at", columnList = "tenant_id, created_at DESC"),
})
public class AuditRecordEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "action", nullable = false, updatable = false, length = 30)
    private String action;

    @Column(name = "diff", nullable = false, updatable = false, columnDefinition = "JSONB")
    private String diff = "{}";

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "correlation_id", updatable = false, length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ─── Panache-required default constructor ────────────────────────

    public AuditRecordEntity() {
    }

    // ─── Lifecycle callbacks ─────────────────────────────────────────

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = (this.createdAt != null) ? this.createdAt : Instant.now();
    }

    // ─── Getters / Setters ───────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMemoryId() { return memoryId; }
    public void setMemoryId(UUID memoryId) { this.memoryId = memoryId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDiff() { return diff; }
    public void setDiff(String diff) { this.diff = diff; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AuditRecordEntity{" +
               "id=" + id +
               ", memoryId=" + memoryId +
               ", tenantId='" + tenantId + '\'' +
               ", action='" + action + '\'' +
               ", userId='" + userId + '\'' +
               '}';
    }
}
