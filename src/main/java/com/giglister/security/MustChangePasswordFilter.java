package com.giglister.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Blocks an authenticated request from an account whose temporary password (see
 * User.mustChangePassword, AdminService.createUser) still hasn't been replaced - a "must"
 * enforced only on the frontend (see RequirePasswordChange) could always be bypassed by
 * calling the API directly with the still-valid temp-password session. Everything except
 * reading GET /api/me (so the frontend can tell who's logged in and show the forced
 * screen) and PUT /api/me/password (changing it, the one way out) gets a 403 here, before
 * it ever reaches a controller. Runs right after JwtAuthFilter, which is what populates
 * the authentication this reads.
 */
@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        AppUserPrincipal principal = CurrentUser.getOrNull();
        if (principal == null || !principal.isMustChangePassword() || isAllowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", HttpServletResponse.SC_FORBIDDEN);
        payload.put("error", "Forbidden");
        payload.put("message", "Bitte ändere zuerst dein temporäres Passwort.");
        objectMapper.writeValue(response.getWriter(), payload);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return (path.equals("/api/me") && "GET".equals(method))
                || (path.equals("/api/me/password") && "PUT".equals(method));
    }
}
