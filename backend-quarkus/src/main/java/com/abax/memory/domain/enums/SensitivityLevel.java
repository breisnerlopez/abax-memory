package com.abax.memory.domain.enums;

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

    /** Restricted access; highest sensitivity tier. Always requires review. */
    RESTRICTED;

    /**
     * Returns {@code true} when this level triggers mandatory
     * human review (BR-006).
     */
    public boolean requiresReview() {
        return this == CONFIDENTIAL || this == RESTRICTED;
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-sensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (SensitivityLevel s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
