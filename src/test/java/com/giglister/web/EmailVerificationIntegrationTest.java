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
 * Registration only needs a username (checked for availability), an email
 * (unique, used for password recovery) and a password. A confirmation email
 * is sent, and login is blocked until the user follows that link.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void usernameAvailabilityIsCheckable() throws Exception {
        mockMvc.perform(get("/api/auth/username-available").param("username", "freshname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        register("taken@example.com", "password123", "TakenName");

        mockMvc.perform(get("/api/auth/username-available").param("username", "TakenName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void registrationRejectsDuplicateEmailAndUsername() throws Exception {
        register("dup@example.com", "password123", "DupUser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "dup@example.com", "password", "password123", "username", "SomeoneElse"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "someoneelse@example.com", "password", "password123", "username", "DupUser"))))
                .andExpect(status().isConflict());
    }

    @Test
    void loginIsBlockedUntilEmailIsVerifiedThenWorksAfterVerification() throws Exception {
        String email = "verifyme@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "password123", "username", "VerifyMe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        // Not verified yet - login must be refused even with the correct password.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "VerifyMe", "password", "password123"))))
                .andExpect(status().isForbidden());

        String token = userRepository.findByEmailIgnoreCase(email).orElseThrow().getVerificationToken();

        // A bogus token must be rejected.
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "not-a-real-token"))))
                .andExpect(status().isBadRequest());

        // The real link both confirms the address and logs the user in.
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("VerifyMe"));

        // From now on, ordinary login works too.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "VerifyMe", "password", "password123"))))
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
