package com.abax.memory.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Universal memory classification types — v2.0.0.
 * <p>
 * These eight kinds form the primary immutable classification axis
 * for any MemoryFragment. Once set at creation time, the kind
 * cannot be changed.
 * </p>
 *
 * <p>The JSON representation uses lowercase (matching the spec and
 * frontend). Java code uses standard UPPER_SNAKE_CASE enum names.</p>
 *
 * <p>References: EP-001, FT-001.01, HU-001.01.1, BR-010</p>
 */
public enum MemoryKind {

    /** Objective, verifiable fact. */
    FACT,

    /** Subjective preference of a user or entity. */
    PREFERENCE,

    /** Something that occurred at a specific point in time. */
    EVENT,

    /** A documented decision with context and rationale. */
    DECISION,

    /** A pending or completed action item. */
    TASK,

    /** Reusable steps, instructions, or procedures. */
    PROCEDURE,

    /** Free-form knowledge without a predefined structure. */
    NOTE,

    /** A named person, organization, system, or concept. */
    ENTITY;

    /**
     * Returns the lowercase JSON representation.
     */
    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }

    /**
     * Factory method for deserialization from JSON string (case-insensitive).
     */
    @JsonCreator
    public static MemoryKind fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (MemoryKind k : values()) {
            if (k.name().equalsIgnoreCase(value)) {
                return k;
            }
        }
        throw new IllegalArgumentException("Unknown MemoryKind: " + value
                + ". Expected one of: fact, preference, event, decision, task, procedure, note, entity");
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-insensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (MemoryKind k : values()) {
            if (k.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
