package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giglister.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the "Admin ernennen" flow: the bootstrap admin (from
 * giglister.admin.bootstrap-email) can list users and promote/demote them,
 * but can never remove their own admin rights.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "giglister.admin.bootstrap-email=admin@giglister.test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminUserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminCanPromoteAndDemoteOtherUsersButNotThemselves() throws Exception {
        String adminToken = register("admin@giglister.test", "adminpass123", "Admin");

        var regularResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "max@example.com",
                                "password", "password123",
                                "username", "Max"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        var regularJson = objectMapper.readTree(regularResult.getResponse().getContentAsString());
        long regularUserId = regularJson.get("userId").asLong();
        verifyEmail("max@example.com");
        long adminUserId = objectMapper.readTree(fetchMe(adminToken)).get("id").asLong();

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'max@example.com')].platformAdmin").value(false));

        mockMvc.perform(post("/api/admin/users/" + regularUserId + "/promote")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformAdmin").value(true));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken).param("q", "max"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platformAdmin").value(true));

        // Self-demotion must be rejected so the platform can never end up without an admin.
        mockMvc.perform(post("/api/admin/users/" + adminUserId + "/demote")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/users/" + regularUserId + "/demote")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformAdmin").value(false));

        // A non-admin must not reach any /api/admin/** endpoint.
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + regularToken(regularUserId)))
                .andExpect(status().isForbidden());
    }

    private String register(String email, String password, String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "username", username))))
                .andExpect(status().isCreated());
        return verifyEmail(email);
    }

    private String verifyEmail(String email) throws Exception {
        String verificationToken = userRepository.findByEmailIgnoreCase(email).orElseThrow().getVerificationToken();
        var result = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationToken))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String fetchMe(String token) throws Exception {
        var result = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String regularToken(long regularUserId) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "Max", "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
