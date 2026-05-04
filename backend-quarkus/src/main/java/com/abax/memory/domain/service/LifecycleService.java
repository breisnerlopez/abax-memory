package com.abax.memory.domain.service;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.model.MemoryFragment;

/**
 * Service contract for lifecycle-state transitions — v2.0.0.
 * <p>
 * Encapsulates the state machine defined in {@link LifecycleState},
 * enforces guard conditions, and records review events.
 * </p>
 */
public interface LifecycleService {

    /**
     * Attempts to transition a memory fragment from its current
     * state to the given target state.
     *
     * @return the updated memory fragment after the transition
     * @throws IllegalStateException if the transition is not allowed
     */
    MemoryFragment transition(MemoryFragment fragment,
                              LifecycleState targetState,
                              String reviewerId,
                              String comment);
}
