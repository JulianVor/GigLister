package com.giglister.web;

import com.giglister.dto.MessageResponse;
import com.giglister.dto.auth.AuthResponse;
import com.giglister.dto.auth.ForgotPasswordRequest;
import com.giglister.dto.auth.LoginRequest;
import com.giglister.dto.auth.RegisterRequest;
import com.giglister.dto.auth.RegisterResponse;
import com.giglister.dto.auth.ResetPasswordRequest;
import com.giglister.dto.auth.UsernameAvailabilityResponse;
import com.giglister.dto.auth.VerifyEmailRequest;
import com.giglister.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/username-available")
    public UsernameAvailabilityResponse usernameAvailable(@RequestParam String username) {
        return new UsernameAvailabilityResponse(authService.usernameAvailable(username));
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request.token());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Always answers the same way, whether or not the email is registered - avoids leaking which addresses have accounts. */
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return new MessageResponse("Falls diese E-Mail-Adresse registriert ist, haben wir einen Link zum Zurücksetzen des Passworts geschickt.");
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request.token(), request.newPassword());
    }
}
