package com.giglister.dto;

import com.giglister.domain.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;

public record PermissionRequest(
        @NotNull Long userId,
        @NotNull PermissionLevel permission
) {
}
