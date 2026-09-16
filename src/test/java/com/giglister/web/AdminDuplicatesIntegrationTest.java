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
 * "Kein Duplikat" (see AdminService.rejectDuplicate) - possibleDuplicates() is a plain
 * pairwise scan with nothing persisted, so without this a pair an admin already looked at
 * and confirmed is NOT a duplicate would keep resurfacing on every later dashboard load.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminDuplicatesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminCanRejectAPossibleDuplicateAndItStopsResurfacing() throws Exception {
        String adminToken = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("promoter@example.com", "password123", "Promoter");

        // Identical names -> similarity 1.0, comfortably over either threshold.
        long firstId = createBand(userToken, "Duplicate Test Band", "Bremen");
        long secondId = createBand(userToken, "Duplicate Test Band", "Bremen");

        mockMvc.perform(get("/api/admin/duplicates").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].entityType").value("BAND"));

        // Non-admins can't dismiss a pair either.
        mockMvc.perform(post("/api/admin/duplicates/reject")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entityType", "BAND", "firstId", firstId, "secondId", secondId))))
                .andExpect(status().isForbidden());

        // Order-independent: reject with the ids swapped relative to how the scan reported them.
        mockMvc.perform(post("/api/admin/duplicates/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entityType", "BAND", "firstId", secondId, "secondId", firstId))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/duplicates").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Rejecting the same pair again is a no-op, not an error or a duplicate row.
        mockMvc.perform(post("/api/admin/duplicates/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entityType", "BAND", "firstId", firstId, "secondId", secondId))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/duplicates").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // A third, unrelated band pair is untouched by the rejection above - the dismissal
        // was scoped to firstId/secondId specifically, not every BAND pair going forward.
        createBand(userToken, "Different Third Band", "Bremen");
        createBand(userToken, "Different Third Band", "Bremen");
        mockMvc.perform(get("/api/admin/duplicates").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private long createBand(String token, String name, String city) throws Exception {
        var result = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "city", city))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registerAsAdmin(String email, String password, String username) throws Exception {
        String token = register(email, password, username);
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setPlatformAdmin(true);
        userRepository.save(user);
        return token;
    }

    private String register(String email, String password, String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "username", username))))
                .andExpect(status().isCreated());

        String verificationToken = userRepository.findByEmailIgnoreCase(email).orElseThrow().getVerificationToken();
        var result = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationToken))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
