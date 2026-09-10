package com.giglister.dto.auth;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String username,
        boolean platformAdmin
) {
}
