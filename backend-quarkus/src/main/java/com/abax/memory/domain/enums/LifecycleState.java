package com.abax.memory.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Six-state lifecycle machine for MemoryFragments — v2.0.0.
 * <p>
 * Every memory fragment advances through a governed sequence of states.
 * Transitions are validated by {@link #canTransitionTo(LifecycleState)}.
 * </p>
 *
 * <h3>State machine</h3>
 * <pre>
 *   [*] → DRAFT
 *   DRAFT → IN_REVIEW  (submit for review)
 *   DRAFT → DELETED     (soft-delete)
 *   IN_REVIEW → APPROVED  (reviewer approves)
 *   IN_REVIEW → DRAFT     (reviewer requests changes)
 *   IN_REVIEW → DELETED   (soft-delete)
 *   APPROVED → DEPRECATED (deprecate)
 *   APPROVED → ARCHIVED   (archive)
 *   APPROVED → DELETED    (soft-delete)
 *   DEPRECATED → APPROVED (re-activate via supersedes)
 *   DEPRECATED → ARCHIVED
 *   DEPRECATED → DELETED
 *   ARCHIVED → DELETED    (soft-delete)
 *   * → DELETED            (admin soft-delete from any state)
 * </pre>
 *
 * <p>References: EP-001, FT-001.02, HU-001.02.1, BR-005</p>
 */
public enum LifecycleState {

    /** Initial state: work in progress, not visible to consumers. */
    DRAFT,

    /** Submitted for human review; awaiting approval or rejection. */
    IN_REVIEW,

    /** Approved and published; visible to all authorized consumers. */
    APPROVED,

    /** Still valid but no longer recommended; superseded by a newer version. */
    DEPRECATED,

    /** Archived for historical reference; out of active circulation. */
    ARCHIVED,

    /** Soft-deleted; excluded from all standard queries. */
    DELETED;

    private static final Set<LifecycleState> SOFT_DELETABLE_FROM =
            EnumSet.of(DRAFT, IN_REVIEW, APPROVED, DEPRECATED, ARCHIVED);

    /**
     * Returns the set of states that {@code this} state can legally
     * transition into.
     */
    public Set<LifecycleState> allowedTransitions() {
        return switch (this) {
            case DRAFT       -> EnumSet.of(IN_REVIEW, DELETED);
            case IN_REVIEW   -> EnumSet.of(APPROVED, DRAFT, DELETED);
            case APPROVED    -> EnumSet.of(DEPRECATED, ARCHIVED, DELETED);
            case DEPRECATED  -> EnumSet.of(APPROVED, ARCHIVED, DELETED);
            case ARCHIVED    -> EnumSet.of(DELETED);
            case DELETED     -> EnumSet.noneOf(LifecycleState.class); // terminal
        };
    }

    /**
     * Checks whether {@code this} state can transition to {@code target}.
     */
    public boolean canTransitionTo(LifecycleState target) {
        return allowedTransitions().contains(target);
    }

    /**
     * Returns all states from which soft-delete is a valid transition.
     */
    public static Set<LifecycleState> softDeletableFrom() {
        return SOFT_DELETABLE_FROM;
    }

    /**
     * Returns {@code true} when this state is considered visible
     * in standard consumer search results.
     */
    public boolean isConsumerVisible() {
        return this == APPROVED;
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-sensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (LifecycleState s : values()) {
            if (s.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
