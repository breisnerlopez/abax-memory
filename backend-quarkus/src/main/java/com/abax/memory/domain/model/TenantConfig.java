package com.abax.memory.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-tenant configuration holding rate limits, profile
 * assignment, and operational parameters — v2.0.0.
 *
 * <p>References: EP-003, FT-003.01, §4 of functional spec</p>
 */
public class TenantConfig {

    /** The tenant identifier derived from the OIDC token. */
    private String tenantId;

    /** Reference to the active domain profile. */
    private UUID profileId;

    private int rateLimitPerMin;
    private int rateLimitUserPerMin;
    private int maxTopK;
    private int maxGraphDepth;
    private int maxBatchSize;
    private Long maxMemories;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public TenantConfig() {
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public int getRateLimitPerMin() { return rateLimitPerMin; }
    public void setRateLimitPerMin(int rateLimitPerMin) { this.rateLimitPerMin = rateLimitPerMin; }

    public int getRateLimitUserPerMin() { return rateLimitUserPerMin; }
    public void setRateLimitUserPerMin(int rateLimitUserPerMin) { this.rateLimitUserPerMin = rateLimitUserPerMin; }

    public int getMaxTopK() { return maxTopK; }
    public void setMaxTopK(int maxTopK) { this.maxTopK = maxTopK; }

    public int getMaxGraphDepth() { return maxGraphDepth; }
    public void setMaxGraphDepth(int maxGraphDepth) { this.maxGraphDepth = maxGraphDepth; }

    public int getMaxBatchSize() { return maxBatchSize; }
    public void setMaxBatchSize(int maxBatchSize) { this.maxBatchSize = maxBatchSize; }

    public Long getMaxMemories() { return maxMemories; }
    public void setMaxMemories(Long maxMemories) { this.maxMemories = maxMemories; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
