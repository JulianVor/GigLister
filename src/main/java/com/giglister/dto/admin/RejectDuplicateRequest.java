package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityType;
import jakarta.validation.constraints.NotNull;

public record RejectDuplicateRequest(
        @NotNull EntityType entityType,
        @NotNull Long firstId,
        @NotNull Long secondId
) {
}
