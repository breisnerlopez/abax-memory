package com.abax.memory.api.dto.v2;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for PUT /api/v2/memories/{id}/review — review workflow.
 *
 * <p>Supports four review actions:
 * <ul>
 *   <li>{@code REQUEST} / {@code SUBMIT} — submit for review (DRAFT → PENDING)</li>
 *   <li>{@code APPROVE} — approve the memory (PENDING → ACTIVE)</li>
 *   <li>{@code REJECT}  — send back for rework (PENDING → DRAFT)</li>
 * </ul>
 *
 * <p>References: UAT-S05, EP-006, HU-001.02.2</p>
 */
public record ReviewRequest(

        @NotNull(message = "action is required (SUBMIT, APPROVE, REJECT, or REQUEST)")
        ReviewAction action,

        String comment
) {

    /**
     * Review actions for the review workflow endpoint.
     *
     * <p>{@code SUBMIT} is synonymous with {@code REQUEST}; both initiate
     * the DRAFT → PENDING transition. Clients may use either value.</p>
     */
    public enum ReviewAction {
        APPROVE,
        REJECT,
        REQUEST,
        SUBMIT;
    }
}
