package com.abax.memory.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to {@code profiles} — domain-profile configurations
 * stored as JSONB that specialize the generic memory engine.
 *
 * <p>Profiles define recommended kinds, suggested tags, default values,
 * and extra metadata fields without modifying core code.</p>
 *
 * <p>References: EP-002, Flyway V5, Architecture document §3.7</p>
 */
@Entity
@Table(name = "profiles", indexes = {
        @Index(name = "idx_profiles_name", columnList = "name"),
})
public class DomainProfileEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "version", nullable = false, length = 10)
    private String version = "1.0";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "config", nullable = false, columnDefinition = "JSONB")
    private String config;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ─── Panache-required default constructor ────────────────────────

    public DomainProfileEntity() {
    }

    // ─── Lifecycle callbacks ─────────────────────────────────────────

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = (this.createdAt != null) ? this.createdAt : now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ─── Convenience finders ─────────────────────────────────────────

    /**
     * Finds a profile by its unique name.
     */
    public static DomainProfileEntity findByName(String name) {
        return find("name", name).firstResult();
    }

    /**
     * Finds the default (first active) profile.
     */
    public static DomainProfileEntity findDefault() {
        return find("active", true).firstResult();
    }

    /**
     * Lists all active profiles.
     */
    public static java.util.List<DomainProfileEntity> listActive() {
        return list("active", true);
    }

    // ─── Getters / Setters ───────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "DomainProfileEntity{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", version='" + version + '\'' +
               ", active=" + active +
               '}';
    }
}
