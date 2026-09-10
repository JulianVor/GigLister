package com.giglister.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper to read the authenticated user's id/admin flag out of the security context. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AppUserPrincipal getOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    public static AppUserPrincipal require() {
        AppUserPrincipal principal = getOrNull();
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return principal;
    }

    public static Long requireId() {
        return require().getId();
    }
}
