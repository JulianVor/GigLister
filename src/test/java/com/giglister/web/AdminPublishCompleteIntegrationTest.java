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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The "Alle auf Vollständigkeit setzen" admin bulk action (see
 * BandService/LocationService.publishAllComplete) - the one path that publishes a
 * STUB/DRAFT band or location once it's complete without an admin having to click
 * publish one row at a time. Its own class (not folded into AdminOverviewIntegrationTest)
 * so the exact checked/published counts below aren't polluted by other tests' leftover
 * STUB/DRAFT rows in the same shared-context database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminPublishCompleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminCanBulkPublishCompleteBandsAndLocations() throws Exception {
        String adminToken = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("promoter@example.com", "password123", "Promoter");

        // Created directly, so it starts as DRAFT regardless of completeness (create()
        // never checks it on its own) - but already clears isComplete's bar (name, country,
        // shortDescription, genres - city is deliberately not part of it).
        mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Complete Draftband", "country", "Deutschland",
                                "shortDescription", "A fully filled-in band",
                                "genres", List.of("Rock")))))
                .andExpect(status().isCreated());

        // Missing country/shortDescription/genres - must stay DRAFT after the sweep.
        mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Incomplete Draftband", "city", "Bremen"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/locations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Complete Draft Venue", "city", "Bremen",
                                "address", "Teststraße 2", "postalCode", "28195"))))
                .andExpect(status().isCreated());

        // Missing postalCode - must stay DRAFT after the sweep.
        mockMvc.perform(post("/api/locations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Incomplete Draft Venue", "city", "Bremen", "address", "Teststraße 3"))))
                .andExpect(status().isCreated());

        // Non-admins can't trigger the sweep either.
        mockMvc.perform(post("/api/admin/bands/publish-complete").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/locations/publish-complete").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/bands/publish-complete").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(2))
                .andExpect(jsonPath("$.published").value(1));

        mockMvc.perform(post("/api/admin/locations/publish-complete").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(2))
                .andExpect(jsonPath("$.published").value(1));

        mockMvc.perform(get("/api/admin/bands").header("Authorization", "Bearer " + adminToken).param("q", "Complete Draftband"))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
        mockMvc.perform(get("/api/admin/bands").header("Authorization", "Bearer " + adminToken).param("q", "Incomplete Draftband"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
        mockMvc.perform(get("/api/admin/locations").header("Authorization", "Bearer " + adminToken).param("q", "Complete Draft Venue"))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
        mockMvc.perform(get("/api/admin/locations").header("Authorization", "Bearer " + adminToken).param("q", "Incomplete Draft Venue"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));

        // Newly-published bands/locations are gone from a fresh sweep - only the still-
        // incomplete one is left to (not) publish.
        mockMvc.perform(post("/api/admin/bands/publish-complete").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(1))
                .andExpect(jsonPath("$.published").value(0));
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
