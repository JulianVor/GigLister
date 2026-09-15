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

import static org.assertj.core.api.Assertions.assertThat;
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

    /**
     * Covers the "Nutzer anlegen" flow: an admin creates an account through the UI (no
     * password field), the response carries a one-time temporary password, the new account
     * can log in with it right away (no email-verification wait, unlike self-registration),
     * and is locked out of everything except reading /api/me and changing its password
     * until it does - after which it behaves like any other account.
     */
    @Test
    void adminCanCreateAUserWhoMustChangeTheTemporaryPasswordBeforeAnythingElse() throws Exception {
        String adminToken = ensureAdminToken();

        var createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "newhire@example.com", "username", "Newhire"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newhire@example.com"))
                .andExpect(jsonPath("$.username").value("Newhire"))
                .andReturn();
        String temporaryPassword = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("temporaryPassword").asText();
        assertThat(temporaryPassword).hasSize(12);

        // No email-verification round trip needed - the admin creating the account with a
        // real address they typed in is itself the verification.
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "Newhire", "password", temporaryPassword))))
                .andExpect(status().isOk())
                .andReturn();
        String newUserToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        // Locked out of everything but reading its own /api/me and changing its password.
        mockMvc.perform(get("/api/bands").header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/me").header("Authorization", "Bearer " + newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeCity", "Bremen"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/me/password").header("Authorization", "Bearer " + newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", temporaryPassword, "newPassword", "myOwnPassword123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        // Free to use the API normally now.
        mockMvc.perform(get("/api/bands").header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk());

        // Duplicate email/username are rejected the same way self-registration rejects them.
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "newhire@example.com", "username", "SomeoneElse"))))
                .andExpect(status().isConflict());
    }

    /** The bootstrap admin ("admin@giglister.test") may already exist from an earlier test
     * method in this class - the DB isn't reset between methods, only after the whole class
     * (see @DirtiesContext) - so this logs in instead of re-registering when that happens,
     * working regardless of which order the test methods actually run in. */
    private String ensureAdminToken() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "Admin", "password", "adminpass123"))))
                .andReturn();
        if (loginResult.getResponse().getStatus() == 200) {
            return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        }
        return register("admin@giglister.test", "adminpass123", "Admin");
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
