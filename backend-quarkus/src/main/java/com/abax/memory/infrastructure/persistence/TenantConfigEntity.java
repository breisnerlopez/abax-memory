package com.abax.memory.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to {@code tenant_configs} — per-tenant configuration,
 * rate limits, and profile assignment.
 *
 * <p>Uses Panache active-record pattern ({@link PanacheEntityBase})
 * for concise repository operations.</p>
 *
 * <p>References: EP-003, Flyway V6, Architecture document §6.1</p>
 */
@Entity
@Table(name = "tenant_configs")
public class TenantConfigEntity extends PanacheEntityBase {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
    private String tenantId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "rate_limit_per_min", nullable = false)
    private int rateLimitPerMin = 1000;

    @Column(name = "rate_limit_user_per_min", nullable = false)
    private int rateLimitUserPerMin = 300;

    @Column(name = "max_top_k", nullable = false)
    private int maxTopK = 100;

    @Column(name = "max_graph_depth", nullable = false)
    private int maxGraphDepth = 5;

    @Column(name = "max_batch_size", nullable = false)
    private int maxBatchSize = 100;

    @Column(name = "max_memories")
    private Long maxMemories;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ─── Panache-required default constructor ────────────────────────

    public TenantConfigEntity() {
    }

    // ─── Lifecycle callbacks ─────────────────────────────────────────

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = (this.createdAt != null) ? this.createdAt : now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ─── Convenience finders ─────────────────────────────────────────

    /**
     * Finds the tenant configuration by tenant ID.
     *
     * @param tenantId tenant identifier
     * @return the config entity or {@code null} if not found
     */
    public static TenantConfigEntity findByTenantId(String tenantId) {
        return find("tenantId", tenantId).firstResult();
    }

    // ─── Getters / Setters ───────────────────────────────────────────

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

    @Override
    public String toString() {
        return "TenantConfigEntity{" +
               "tenantId='" + tenantId + '\'' +
               ", active=" + active +
               ", rateLimitPerMin=" + rateLimitPerMin +
               '}';
    }
}
