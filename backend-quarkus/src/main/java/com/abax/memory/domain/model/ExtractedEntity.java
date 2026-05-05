package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.MemoryKind;

import java.util.List;

/**
 * Represents a named entity extracted from memory content via LLM.
 *
 * <p>References: FT-001.04, HU-001.04.1</p>
 */
public record ExtractedEntity(
        /** Canonical name of the entity. */
        String name,

        /** Entity type: PERSON, SYSTEM, DATE, TICKET, METRIC, TECHNOLOGY, PLATFORM, TOOL, CUSTOM. */
        String type,

        /** Confidence score [0.0, 1.0] for this extraction. */
        double confidence
) {}
