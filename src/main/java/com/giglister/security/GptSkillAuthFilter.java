package com.giglister.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        if (configuredToken != null && !configuredToken.isBlank()
                && header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null
                && constantTimeEquals(header.substring(7), configuredToken)) {
            var authToken = new UsernamePasswordAuthenticationToken(
                    "gpt-skill", null, List.of(new SimpleGrantedAuthority("ROLE_GPT_SKILL")));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
