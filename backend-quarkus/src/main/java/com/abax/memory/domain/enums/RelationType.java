package com.abax.memory.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Nine typed, directed relationships between MemoryFragments — v2.0.0.
 * <p>
 * Each type carries a specific semantic and directionality that
 * the graph traversal layer respects.
 * </p>
 *
 * <p>References: EP-001, FT-001.03, HU-001.03.1</p>
 */
public enum RelationType {

    /** Generic bidirectional connection. */
    RELATED_TO(Directionality.BIDIRECTIONAL),

    /** A depends on B for its existence or meaning. Directed A → B. */
    DEPENDS_ON(Directionality.DIRECTED_FORWARD),

    /** A was caused or triggered by B. Directed A → B. */
    CAUSED_BY(Directionality.DIRECTED_FORWARD),

    /** A resolves, fixes, or answers B. Directed A → B. */
    RESOLVES(Directionality.DIRECTED_FORWARD),

    /** A contradicts or is incompatible with B. Bidirectional. */
    CONTRADICTS(Directionality.BIDIRECTIONAL),

    /** A provides evidence, support, or justification for B. Directed A → B. */
    SUPPORTS(Directionality.DIRECTED_FORWARD),

    /** A mentions or references entity B. Directed A → B. */
    MENTIONS(Directionality.DIRECTED_FORWARD),

    /** A belongs to the group, collection, or category B. Directed A → B. */
    BELONGS_TO(Directionality.DIRECTED_FORWARD),

    /** A is a newer version that supersedes/replaces B. Directed A → B. */
    SUPERSEDES(Directionality.DIRECTED_FORWARD);

    public enum Directionality {
        /** Edge is meaningful in both directions. */
        BIDIRECTIONAL,
        /** Edge is meaningful only source → target. */
        DIRECTED_FORWARD
    }

    private final Directionality directionality;

    RelationType(Directionality directionality) {
        this.directionality = directionality;
    }

    public Directionality directionality() {
        return directionality;
    }

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
    public static RelationType fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (RelationType r : values()) {
            if (r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown RelationType: " + value
                + ". Expected one of: related_to, depends_on, caused_by, resolves, contradicts, supports, mentions, belongs_to, supersedes");
    }

    /**
     * Returns {@code true} when this relation type can be traversed
     * in reverse (target → source).
     */
    public boolean isTraversableReverse() {
        return directionality == Directionality.BIDIRECTIONAL;
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-insensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (RelationType r : values()) {
            if (r.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
