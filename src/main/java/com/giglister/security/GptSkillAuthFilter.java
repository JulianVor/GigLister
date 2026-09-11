package com.giglister.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates the external GPT-skill integration via a single shared
 * secret (GIGLISTER_GPT_SKILL_TOKEN) instead of a real user account - there
 * is exactly one external caller, not many, so a full API-token/user system
 * would be over-engineering. This identity is only ever authorized for
 * POST /api/submissions (see SecurityConfig): it can propose content, never
 * create anything directly.
 */
@Slf4j
@Component
public class GptSkillAuthFilter extends OncePerRequestFilter {

    private final String configuredToken;

    public GptSkillAuthFilter(@Value("${giglister.gpt-skill.token:}") String configuredToken) {
        this.configuredToken = configuredToken;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        boolean matched = configuredToken != null && !configuredToken.isBlank()
                && header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null
                && constantTimeEquals(header.substring(7), configuredToken);
        if (matched) {
            var authToken = new UsernamePasswordAuthenticationToken(
                    "gpt-skill", null, List.of(new SimpleGrantedAuthority("ROLE_GPT_SKILL")));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        // TEMPORARY diagnostic logging - remove once the 403-via-Caddy issue is
        // resolved. Never logs the actual token/secret, only lengths/prefixes.
        if (request.getRequestURI().startsWith("/api/submissions")) {
            String received = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
            log.info("[GPT-SKILL DEBUG] method={} uri={} hasAuthHeader={} startsWithBearer={} "
                            + "configuredTokenLen={} configuredTokenPrefix={} receivedTokenLen={} "
                            + "receivedTokenPrefix={} alreadyAuthenticated={} matched={}",
                    request.getMethod(), request.getRequestURI(),
                    header != null, header != null && header.startsWith("Bearer "),
                    configuredToken == null ? -1 : configuredToken.length(),
                    configuredToken == null || configuredToken.isBlank() ? "" : configuredToken.substring(0, Math.min(6, configuredToken.length())),
                    received == null ? -1 : received.length(),
                    received == null || received.isBlank() ? "" : received.substring(0, Math.min(6, received.length())),
                    SecurityContextHolder.getContext().getAuthentication() != null,
                    matched);
        }
        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
