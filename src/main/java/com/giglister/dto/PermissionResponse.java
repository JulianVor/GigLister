package com.giglister.dto;

import com.giglister.domain.enums.PermissionLevel;

public record PermissionResponse(
        Long userId,
        String email,
        String displayName,
        PermissionLevel permission
) {
}
