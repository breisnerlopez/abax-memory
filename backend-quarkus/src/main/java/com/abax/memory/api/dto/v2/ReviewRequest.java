package com.abax.memory.api.dto.v2;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for PUT /api/v2/memories/{id}/review — review workflow.
 *
 * <p>Supports three review actions:
 * <ul>
 *   <li>{@code REQUEST} — submit for review (DRAFT → PENDING)</li>
 *   <li>{@code APPROVE} — approve the memory (PENDING → ACTIVE)</li>
 *   <li>{@code REJECT}  — send back for rework (PENDING → DRAFT)</li>
 * </ul>
 *
 * <p>References: UAT-S05, EP-006, HU-001.02.2</p>
 */
public record ReviewRequest(

        @NotNull(message = "action is required (APPROVE, REJECT, or REQUEST)")
        ReviewAction action,

        String comment
) {

    /**
     * Review actions for the review workflow endpoint.
     */
    public enum ReviewAction {
        APPROVE,
        REJECT,
        REQUEST;
    }
}
