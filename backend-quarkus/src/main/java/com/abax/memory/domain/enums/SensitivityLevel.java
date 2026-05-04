package com.abax.memory.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Four-tier sensitivity classification — v2.0.0.
 * <p>
 * Governs visibility and access-control decisions. Higher levels
 * may trigger mandatory human review (BR-006).
 * </p>
 *
 * <p>References: EP-001, FT-001.09, HU-001.09.1</p>
 */
public enum SensitivityLevel {

    /** Publicly accessible; no restrictions. */
    PUBLIC,

    /** Internal use within the organization. Default when no profile applies. */
    INTERNAL,

    /** Confidential information; limited distribution. */
    CONFIDENTIAL,

    /** Secret information; maximum restriction. Always requires review. */
    SECRET;

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
    public static SensitivityLevel fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (SensitivityLevel s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown SensitivityLevel: " + value
                + ". Expected one of: public, internal, confidential, secret");
    }

    /**
     * Returns {@code true} when this level triggers mandatory
     * human review (BR-006).
     */
    public boolean requiresReview() {
        return this == CONFIDENTIAL || this == SECRET;
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-insensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (SensitivityLevel s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
