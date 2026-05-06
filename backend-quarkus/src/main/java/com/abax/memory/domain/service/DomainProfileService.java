package com.abax.memory.domain.service;

import com.abax.memory.domain.model.DomainProfile;

import java.util.List;

/**
 * Service contract for domain profile management — v2.1.0.
 *
 * <p>Domain profiles are JSON-configurable specialization layers
 * over the generic memory engine. They define recommended kinds,
 * suggested tags, default values, and extra metadata fields
 * without modifying core code.</p>
 *
 * <p>New in v2.1.0 — CP-V21-041+ (Gap 2: Domain profile management API).</p>
 *
 * <p>References: EP-002, CP-V21-041+, §3 of functional spec</p>
 */
public interface DomainProfileService {

    /**
     * Lists all domain profiles (active and inactive).
     *
     * @return list of all domain profiles
     */
    List<DomainProfile> listAll();

    /**
     * Lists only active domain profiles.
     *
     * @return list of active domain profiles
     */
    List<DomainProfile> listActive();

    /**
     * Retrieves a domain profile by its unique name.
     *
     * @param name profile name
     * @return the profile, or {@code null} if not found
     */
    DomainProfile getByName(String name);

    /**
     * Creates or updates a domain profile.
     *
     * <p>If a profile with the given name already exists, it is updated.
     * Otherwise, a new profile is created.</p>
     *
     * @param name        profile name (unique identifier)
     * @param config      JSON configuration as a string (will be stored in JSONB)
     * @param description human-readable description
     * @param version     version string (e.g., "1.0")
     * @param active      whether the profile is active
     * @param actorId     identity of the user performing the operation
     * @return the created or updated profile
     */
    DomainProfile createOrUpdate(String name, String config, String description,
                                 String version, boolean active, String actorId);

    /**
     * Deletes a domain profile by name (admin-only).
     *
     * @param name    profile name to delete
     * @param actorId identity of the user performing the delete
     * @throws jakarta.ws.rs.NotFoundException if profile not found
     */
    void delete(String name, String actorId);
}
