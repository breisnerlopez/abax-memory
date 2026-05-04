package com.abax.memory.domain.service;

import com.abax.memory.domain.model.Relation;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for typed relationships between MemoryFragments.
 */
public interface RelationService {

    /** Creates a new directed relationship. */
    Relation create(Relation relation);

    /** Deletes a relationship by its id. */
    void delete(UUID relationId);

    /** Lists all relationships where the given memory is the source. */
    List<Relation> findBySource(UUID memoryId);

    /** Lists all relationships where the given memory is the target. */
    List<Relation> findByTarget(UUID memoryId);
}
