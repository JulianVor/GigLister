package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityType;
import jakarta.validation.constraints.NotNull;

public record MergeRequest(
        @NotNull EntityType entityType,
        @NotNull Long sourceEntityId,
        @NotNull Long targetEntityId
) {
}
