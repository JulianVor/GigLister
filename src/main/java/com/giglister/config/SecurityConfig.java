package com.giglister.config;

import com.giglister.security.GptSkillAuthFilter;
import com.giglister.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final GptSkillAuthFilter gptSkillAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot's own error controller, reached via an internal
                        // forward whenever a request handler throws something
                        // GlobalExceptionHandler doesn't catch - that forward doesn't
                        // carry over the original request's authentication (our filters
                        // don't re-run for it), so without this the real error response
                        // gets masked by a second, misleading 403 from THIS request being
                        // "unauthenticated" instead - see /api/submissions below, whose
                        // real failure this was hiding.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/**", "/api/locations/**", "/api/bands/**",
                                "/api/search/**", "/api/discover/**", "/uploads/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // The GPT-skill integration: can only ever propose a submission, never
                        // create a Band/Location/Event directly (see GptSkillAuthFilter).
                        .requestMatchers(HttpMethod.POST, "/api/submissions").hasRole("GPT_SKILL")
                        .anyRequest().authenticated()
                )
                // TEMPORARY diagnostic logging - remove once the 403-via-Caddy issue is
                // resolved. Logs exactly what SecurityContextHolder holds at the moment
                // Spring Security actually denies a request, since GptSkillAuthFilter's own
                // logging shows it successfully sets ROLE_GPT_SKILL yet the response is
                // still 403 - this pins down whether that authentication is somehow gone by
                // the time AuthorizationFilter runs (entry point = never authenticated at
                // all) or present but missing the role (access-denied handler), or something
                // else is going on. Both handlers still respond 403, matching Spring
                // Security's own default Http403ForbiddenEntryPoint exactly, so this is
                // observationally a no-op besides the extra log line.
                .exceptionHandling(handling -> handling
                        .accessDeniedHandler((request, response, ex) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            log.warn("[ACCESS-DENIED DEBUG] uri={} method={} authPresent={} authorities={} authClass={}",
                                    request.getRequestURI(), request.getMethod(),
                                    auth != null, auth == null ? "null" : auth.getAuthorities(),
                                    auth == null ? "null" : auth.getClass().getSimpleName());
                            response.setStatus(403);
                        })
                        .authenticationEntryPoint((request, response, ex) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            log.warn("[AUTH-ENTRY-POINT DEBUG] uri={} method={} authPresent={} message={}",
                                    request.getRequestURI(), request.getMethod(), auth != null, ex.getMessage());
                            response.setStatus(403);
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(gptSkillAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
