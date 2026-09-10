package com.giglister.dto.admin;

public record AdminUserResponse(
        Long id,
        String email,
        String displayName,
        boolean platformAdmin
) {
}
