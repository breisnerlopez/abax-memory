package com.abax.memory.domain.enums;

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
    RELATES_TO(Directionality.BIDIRECTIONAL),

    /** A depends on B for its existence or meaning. Directed A → B. */
    DEPENDS_ON(Directionality.DIRECTED_FORWARD),

    /** A is blocked or prevented by B. Directed A → B. */
    BLOCKED_BY(Directionality.DIRECTED_FORWARD),

    /** A resolves, fixes, or answers B. Directed A → B. */
    RESOLVES(Directionality.DIRECTED_FORWARD),

    /** A is a newer version that supersedes B. Directed A → B. */
    SUPERSEDES(Directionality.DIRECTED_FORWARD),

    /** A references or cites B (weak connection). Directed A → B. */
    REFERENCES(Directionality.DIRECTED_FORWARD),

    /** A is derived or inferred from B. Directed A → B. */
    DERIVES_FROM(Directionality.DIRECTED_FORWARD),

    /** A and B contradict each other. Bidirectional. */
    CONTRADICTS(Directionality.BIDIRECTIONAL),

    /** A provides evidence, support, or justification for B. Directed A → B. */
    SUPPORTS(Directionality.DIRECTED_FORWARD);

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
     * Returns {@code true} when this relation type can be traversed
     * in reverse (target → source).
     */
    public boolean isTraversableReverse() {
        return directionality == Directionality.BIDIRECTIONAL;
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-sensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (RelationType r : values()) {
            if (r.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
