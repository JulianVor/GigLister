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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * "Details ansehen" on a user from the admin side (see AdminService.userDetail) - reuses
 * MeResponse's own shape, so this covers that the admin sees exactly the same gemerkte
 * Konzerte / gefolgte Bands a user sees about themselves under Mein GigLister.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminUserDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminCanSeeAUsersFollowedBandsAndSavedConcerts() throws Exception {
        String adminToken = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("fan@example.com", "password123", "Fan");
        Long userId = userRepository.findByEmailIgnoreCase("fan@example.com").orElseThrow().getId();

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(10).toString(),
                "location", Map.of("name", "Detail Venue", "city", "Bremen", "address", "Teststraße 5", "postalCode", "28195"),
                "bands", List.of(Map.of("name", "Detail Band", "city", "Bremen"))
        );
        var eventResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var eventJson = objectMapper.readTree(eventResult.getResponse().getContentAsString()).get("event");
        long eventId = eventJson.get("id").asLong();
        long bandId = eventJson.get("bands").get(0).get("id").asLong();

        mockMvc.perform(post("/api/bands/" + bandId + "/follow").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/events/" + eventId + "/save").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Non-admins can't look at someone else's detail.
        mockMvc.perform(get("/api/admin/users/" + userId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users/" + userId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Fan"))
                .andExpect(jsonPath("$.email").value("fan@example.com"))
                .andExpect(jsonPath("$.followedBands.length()").value(1))
                .andExpect(jsonPath("$.followedBands[0].id").value(bandId))
                .andExpect(jsonPath("$.followedBands[0].name").value("Detail Band"))
                .andExpect(jsonPath("$.savedEvents.length()").value(1))
                .andExpect(jsonPath("$.savedEvents[0].id").value(eventId));

        // A nonexistent user id 404s rather than a null/empty response.
        mockMvc.perform(get("/api/admin/users/999999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
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
