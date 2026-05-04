package com.abax.memory.domain.enums;

/**
 * Universal memory classification types — v2.0.0.
 * <p>
 * These eight kinds form the primary immutable classification axis
 * for any MemoryFragment. Once set at creation time, the kind
 * cannot be changed.
 * </p>
 *
 * <p>References: EP-001, FT-001.01, HU-001.01.1</p>
 */
public enum MemoryKind {

    /** A formal decision documented with context and rationale. */
    DECISION,

    /** An operational incident: outage, alert, degradation event. */
    INCIDENT,

    /** A named entity: person, organization, system, service, concept. */
    ENTITY,

    /** General knowledge: fact, definition, reference material. */
    KNOWLEDGE,

    /** A product feature: capability, behavior, user-facing function. */
    FEATURE,

    /** Conversational memory produced by or for an AI agent. */
    AGENT_MEMORY,

    /** A document: policy, runbook, guide, manual. */
    DOCUMENT,

    /** Custom domain-specific memory type (extensible via profiles). */
    CUSTOM;

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-sensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (MemoryKind k : values()) {
            if (k.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
