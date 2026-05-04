package com.abax.memory.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Six-state lifecycle machine for MemoryFragments — v2.0.0.
 * <p>
 * Every memory fragment advances through a governed sequence of states.
 * Transitions are validated by {@link #canTransitionTo(LifecycleState)}.
 * </p>
 *
 * <h3>State machine (matching spec BR-005)</h3>
 * <pre>
 *   [*] → DRAFT
 *   DRAFT → PENDING   (submit for review)
 *   DRAFT → DELETED    (soft-delete)
 *   PENDING → ACTIVE   (reviewer approves)
 *   PENDING → REJECTED (reviewer rejects)
 *   PENDING → DRAFT    (reviewer requests changes)
 *   PENDING → DELETED  (soft-delete)
 *   ACTIVE → ARCHIVED  (archive)
 *   ACTIVE → DELETED   (soft-delete)
 *   REJECTED → DRAFT   (resubmit after rejection)
 *   REJECTED → DELETED (soft-delete)
 *   ARCHIVED → DELETED (soft-delete)
 *   DELETED → terminal (no further transitions)
 * </pre>
 *
 * <p>References: EP-001, FT-001.02, HU-001.02.1, BR-005</p>
 */
public enum LifecycleState {

    /** Initial state: work in progress, not visible to consumers. */
    DRAFT,

    /** Submitted for human review; awaiting approval or rejection. */
    PENDING,

    /** Approved and published; visible to all authorized consumers. */
    ACTIVE,

    /** Rejected in review; requires iteration by the creator. */
    REJECTED,

    /** Archived for historical reference; out of active circulation. */
    ARCHIVED,

    /** Soft-deleted; excluded from all standard queries. */
    DELETED;

    private static final Set<LifecycleState> SOFT_DELETABLE_FROM =
            EnumSet.of(DRAFT, PENDING, ACTIVE, REJECTED, ARCHIVED);

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
    public static LifecycleState fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (LifecycleState s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown LifecycleState: " + value
                + ". Expected one of: draft, pending, active, rejected, archived, deleted");
    }

    /**
     * Returns the set of states that {@code this} state can legally
     * transition into.
     */
    public Set<LifecycleState> allowedTransitions() {
        return switch (this) {
            case DRAFT      -> EnumSet.of(PENDING, DELETED);
            case PENDING    -> EnumSet.of(ACTIVE, REJECTED, DRAFT, DELETED);
            case ACTIVE     -> EnumSet.of(ARCHIVED, DELETED);
            case REJECTED   -> EnumSet.of(DRAFT, DELETED);
            case ARCHIVED   -> EnumSet.of(DELETED);
            case DELETED    -> EnumSet.noneOf(LifecycleState.class); // terminal
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
        return this == ACTIVE;
    }

    /**
     * Returns {@code true} if the given string matches one of the enum constants
     * (case-insensitive).
     */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (LifecycleState s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
