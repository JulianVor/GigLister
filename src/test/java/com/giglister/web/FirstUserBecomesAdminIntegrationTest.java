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
 * A brand new deployment has no way to reach an admin at all otherwise, so
 * the very first account ever registered becomes PLATFORM_ADMIN
 * automatically - but only that one; everyone after it is a normal user,
 * same as GIGLISTER_ADMIN_EMAIL not matching them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FirstUserBecomesAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void onlyTheVeryFirstRegisteredAccountBecomesAdmin() throws Exception {
        String firstToken = register("first@example.com", "password123", "FirstUser");
        String secondToken = register("second@example.com", "password123", "SecondUser");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationTokenFor("first@example.com")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformAdmin").value(true));

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationTokenFor("second@example.com")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformAdmin").value(false));

        // The first user can now reach admin-only endpoints; the second cannot.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "first@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformAdmin").value(true));

        assertAdminAccess("first@example.com", true);
        assertAdminAccess("second@example.com", false);
    }

    private void assertAdminAccess(String email, boolean expectAdmin) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        var status = expectAdmin ? status().isOk() : status().isForbidden();
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status);
    }

    private String verificationTokenFor(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getVerificationToken();
    }

    private String register(String email, String password, String username) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "username", username))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("userId").asText();
    }
}
