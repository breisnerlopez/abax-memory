package com.abax.memory.domain.model;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.MemoryKind;

import java.util.List;
import java.util.UUID;

/**
 * Result of LLM-based memory validation.
 *
 * <p>References: BR-006, BR-005</p>
 */
public record ValidationResult(
        /** Whether the memory passes validation. */
        boolean isValid,

        /** List of issues found (empty if valid). */
        List<String> issues,

        /** Suggested lifecycle state based on content analysis. */
        LifecycleState suggestedLifecycle,

        /** Suggested kind based on content analysis. */
        MemoryKind suggestedKind,

        /** ID of a potential duplicate memory, or null. */
        UUID duplicateOf
) {

    /** Convenience factory for a passing validation. */
    public static ValidationResult valid() {
        return new ValidationResult(true, List.of(), null, null, null);
    }

    /** Convenience factory for a failed validation with issues. */
    public static ValidationResult invalid(List<String> issues) {
        return new ValidationResult(false, issues, null, null, null);
    }
}
