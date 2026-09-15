package com.giglister.dto.admin;

/** `temporaryPassword` is only ever returned here, once, right after creation - it's never
 * stored in plaintext and there's no way to retrieve it again afterward, same as e.g. a
 * GitHub personal access token. The admin has to hand it to the new user themselves. */
public record AdminCreateUserResponse(
        Long id,
        String email,
        String username,
        String temporaryPassword
) {
}
