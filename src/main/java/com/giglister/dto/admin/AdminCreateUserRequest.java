package com.giglister.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** No password field - AdminService.createUser generates a temporary one, same as
 * inviting someone rather than having them self-register. */
public record AdminCreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,30}$",
                message = "must be 3-30 characters and contain only letters, digits, '.', '_' or '-'")
        String username
) {
}
