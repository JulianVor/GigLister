package com.giglister.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,30}$",
                message = "must be 3-30 characters and contain only letters, digits, '.', '_' or '-'")
        String username
) {
}
