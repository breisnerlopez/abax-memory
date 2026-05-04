package com.abax.memory.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain profile: a JSON-configurable specialization layer
 * over the generic memory engine — v2.0.0.
 *
 * <p>Profiles define recommended kinds, suggested tags, default
 * values, and extra metadata fields without modifying core code.</p>
 *
 * <p>References: EP-002, FT-002.01, §3 of functional spec</p>
 */
public class DomainProfile {

    private UUID id;
    private String name;
    private String version;
    private String description;
    private Map<String, Object> config;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public DomainProfile() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
