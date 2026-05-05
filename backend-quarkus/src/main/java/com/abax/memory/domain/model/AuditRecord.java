package com.abax.memory.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit record documenting a single mutation
 * on a MemoryFragment — v2.0.0.
 *
 * <p>Append-only by design. No update or delete operations
 * are permitted on audit records.</p>
 *
 * <p>References: EP-006, FT-006.01, §7.1 of functional spec</p>
 */
public class AuditRecord {

    private UUID id;
    private UUID memoryId;
    private String tenantId;
    private String userId;
    private String action;
    private Map<String, Object> diff;
    private String ipAddress;
    private String userAgent;
    private String correlationId;
    private Instant createdAt;

    public AuditRecord() {
    }

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

    public Map<String, Object> getDiff() { return diff; }
    public void setDiff(Map<String, Object> diff) { this.diff = diff; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
