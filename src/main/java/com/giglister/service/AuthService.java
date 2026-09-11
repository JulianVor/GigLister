package com.giglister.service;

import com.giglister.domain.User;
import com.giglister.dto.auth.AuthResponse;
import com.giglister.dto.auth.LoginRequest;
import com.giglister.dto.auth.RegisterRequest;
import com.giglister.dto.auth.RegisterResponse;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.ConflictException;
import com.giglister.exception.ForbiddenException;
import com.giglister.repository.UserRepository;
import com.giglister.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long VERIFICATION_TOKEN_VALID_HOURS = 24;
    private static final long RESET_TOKEN_VALID_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    @Value("${giglister.admin.bootstrap-email:}")
    private String bootstrapAdminEmail;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("This username is already taken");
        }
        // Two ways to bootstrap the first PLATFORM_ADMIN: an explicit configured
        // email (works regardless of registration order), or - since a fresh
        // deployment otherwise has no way at all to reach an admin - simply
        // being the very first account ever created. Once any user exists, this
        // path never opens again.
        boolean isFirstUser = userRepository.count() == 0;
        boolean matchesBootstrapEmail = !bootstrapAdminEmail.isBlank()
                && bootstrapAdminEmail.equalsIgnoreCase(request.email());
        boolean isBootstrapAdmin = isFirstUser || matchesBootstrapEmail;

        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .username(request.username())
                .platformAdmin(isBootstrapAdmin)
                .emailVerified(false)
                .verificationToken(token)
                .verificationTokenExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_VALID_HOURS, ChronoUnit.HOURS))
                .build();
        user = userRepository.save(user);

        mailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);

        return new RegisterResponse(user.getId(), user.getEmail(),
                "Konto erstellt. Bitte bestätige deine E-Mail-Adresse über den Link, den wir dir geschickt haben.");
    }

    public boolean usernameAvailable(String username) {
        return !userRepository.existsByUsernameIgnoreCase(username);
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Ungültiger Bestätigungslink"));
        if (user.getVerificationTokenExpiresAt() == null || user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Der Bestätigungslink ist abgelaufen");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        user = userRepository.save(user);
        return toAuthResponse(user);
    }

    /** Always succeeds silently, whether or not the email is registered - so this endpoint can't be used to enumerate accounts. */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_VALID_HOURS, ChronoUnit.HOURS));
            userRepository.save(user);
            mailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
        });
    }

    @Transactional
    public AuthResponse resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BadRequestException("Ungültiger Link zum Zurücksetzen des Passworts"));
        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Der Link zum Zurücksetzen des Passworts ist abgelaufen");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        // Following an emailed link proves control of the inbox, same as the verification flow.
        user.setEmailVerified(true);
        user = userRepository.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Bitte bestätige zuerst deine E-Mail-Adresse. Wir haben dir einen Bestätigungslink geschickt.");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getUsername(), user.isPlatformAdmin());
    }
}
