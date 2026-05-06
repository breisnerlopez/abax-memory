package com.abax.memory.infrastructure.service;

import com.abax.memory.domain.model.DomainProfile;
import com.abax.memory.domain.service.DomainProfileService;
import com.abax.memory.infrastructure.persistence.DomainProfileEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link DomainProfileService} backed by the
 * {@code profiles} PostgreSQL table via Panache active-record.
 *
 * <p>New in v2.1.0 — CP-V21-041+ (Gap 2: Domain profile management API).</p>
 *
 * <p>References: EP-002, CP-V21-041+, Flyway V5</p>
 */
@ApplicationScoped
public class DomainProfileServiceImpl implements DomainProfileService {

    private static final Logger LOG = Logger.getLogger(DomainProfileServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public List<DomainProfile> listAll() {
        return DomainProfileEntity.listAll()
                .stream()
                .map(e -> toDomain((DomainProfileEntity) e))
                .toList();
    }

    @Override
    public List<DomainProfile> listActive() {
        return DomainProfileEntity.listActive()
                .stream()
                .map(e -> toDomain((DomainProfileEntity) e))
                .toList();
    }

    @Override
    public DomainProfile getByName(String name) {
        var entity = DomainProfileEntity.findByName(name);
        if (entity == null) {
            return null;
        }
        return toDomain(entity);
    }

    @Override
    @Transactional
    public DomainProfile createOrUpdate(String name, String config, String description,
                                        String version, boolean active, String actorId) {
        var existing = DomainProfileEntity.findByName(name);
        DomainProfileEntity entity;

        if (existing != null) {
            // Update existing profile
            entity = existing;
            entity.setConfig(config);
            entity.setDescription(description);
            entity.setVersion(version != null ? version : entity.getVersion());
            entity.setActive(active);
            entity.persist();
            LOG.infov("Domain profile updated: name={0}, version={1}, active={2}, actor={3}",
                    name, entity.getVersion(), active, actorId);
        } else {
            // Create new profile
            entity = new DomainProfileEntity();
            entity.setId(UUID.randomUUID());
            entity.setName(name);
            entity.setConfig(config);
            entity.setDescription(description);
            entity.setVersion(version != null ? version : "1.0");
            entity.setActive(active);
            entity.persist();
            LOG.infov("Domain profile created: name={0}, version={1}, active={2}, actor={3}",
                    name, entity.getVersion(), active, actorId);
        }

        return toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(String name, String actorId) {
        var entity = DomainProfileEntity.findByName(name);
        if (entity == null) {
            throw new NotFoundException("Domain profile not found: " + name);
        }
        entity.delete();
        LOG.infov("Domain profile deleted: name={0}, actor={1}", name, actorId);
    }

    // ── Entity ↔ Domain mapping ─────────────────────────────────────

    private DomainProfile toDomain(DomainProfileEntity entity) {
        var profile = new DomainProfile();
        profile.setId(entity.getId());
        profile.setName(entity.getName());
        profile.setVersion(entity.getVersion());
        profile.setDescription(entity.getDescription());
        profile.setConfig(deserializeConfig(entity.getConfig()));
        profile.setActive(entity.isActive());
        profile.setCreatedAt(entity.getCreatedAt());
        profile.setUpdatedAt(entity.getUpdatedAt());
        return profile;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            LOG.errorv("Failed to deserialize profile config: {0}", e.getMessage());
            return Map.of();
        }
    }
}
