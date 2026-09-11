package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giglister.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * "Passwort vergessen": requesting a reset link never reveals whether the
 * email is registered, the real link both sets a new password and logs the
 * user in, and it also counts as proving email ownership (same as the
 * verification link), so a never-verified account can log in normally
 * afterwards too.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void forgotPasswordAnswersIdenticallyWhetherOrNotTheEmailIsRegistered() throws Exception {
        register("hasaccount@example.com", "password123", "HasAccount");

        String registeredBody = forgotPassword("hasaccount@example.com");
        String unregisteredBody = forgotPassword("nobody-here@example.com");

        // Same generic message either way - the response must not leak which emails exist.
        org.junit.jupiter.api.Assertions.assertEquals(registeredBody, unregisteredBody);

        // But only the registered user actually got a reset token.
        var user = userRepository.findByEmailIgnoreCase("hasaccount@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(user.getResetToken());
    }

    @Test
    void resetPasswordSetsNewPasswordLogsInAndVerifiesEmail() throws Exception {
        String email = "forgetful@example.com";
        register(email, "originalpass1", "Forgetful");
        verifyEmail(email);

        forgotPassword(email);
        String resetToken = userRepository.findByEmailIgnoreCase(email).orElseThrow().getResetToken();
        org.junit.jupiter.api.Assertions.assertNotNull(resetToken);

        // A bogus token must be rejected.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "not-a-real-token", "newPassword", "brandnewpass1"))))
                .andExpect(status().isBadRequest());

        // The real link sets the new password and logs the user in directly.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", resetToken, "newPassword", "brandnewpass1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("Forgetful"));

        // The old password no longer works ...
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "Forgetful", "password", "originalpass1"))))
                .andExpect(status().isUnauthorized());

        // ... but the new one does.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "Forgetful", "password", "brandnewpass1"))))
                .andExpect(status().isOk());

        // The reset token must be single-use.
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", resetToken, "newPassword", "yetanotherpass1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resettingPasswordCountsAsEmailVerificationForAnUnverifiedAccount() throws Exception {
        String email = "neververified@example.com";
        register(email, "originalpass1", "NeverVerified");
        // Deliberately skip verify-email - the account is still unverified here.

        forgotPassword(email);
        String resetToken = userRepository.findByEmailIgnoreCase(email).orElseThrow().getResetToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", resetToken, "newPassword", "brandnewpass1"))))
                .andExpect(status().isOk());

        // Proving inbox ownership via the reset link should unblock ordinary login too.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "NeverVerified", "password", "brandnewpass1"))))
                .andExpect(status().isOk());
    }

    private String forgotPassword(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void verifyEmail(String email) throws Exception {
        String token = userRepository.findByEmailIgnoreCase(email).orElseThrow().getVerificationToken();
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk());
    }

    private void register(String email, String password, String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "username", username))))
                .andExpect(status().isCreated());
    }
}
