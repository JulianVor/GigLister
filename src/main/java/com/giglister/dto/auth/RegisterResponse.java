package com.giglister.dto.auth;

public record RegisterResponse(
        Long userId,
        String email,
        String message
) {
}
