package com.abax.memory.api.dto.v2;

import com.abax.memory.domain.enums.LifecycleState;
import com.abax.memory.domain.enums.SensitivityLevel;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for PUT /api/v2/memories/{id} — partial update of a MemoryFragment.
 *
 * <p>All fields are optional. Only non-null fields are applied to the entity.
 * A {@code null} field means "keep existing value".</p>
 *
 * <p>References: HU-004.3.1, API Design §7.2</p>
 */
public record UpdateMemoryRequest(

        @Size(max = 500, message = "title must not exceed 500 characters")
        String title,

        String content,

        String summary,

        LifecycleState lifecycleState,

        SensitivityLevel sensitivityLevel,

        Double confidence
) {
}
