package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the admin "see everything, including data that still needs completing"
 * overview: /api/admin/bands|locations|events must surface every status (not
 * just PUBLISHED, unlike the public endpoints), filterable by status/query.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "giglister.admin.bootstrap-email=admin@giglister.test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminOverviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminSeesAllBandsLocationsAndEventsAcrossEveryStatus() throws Exception {
        String adminToken = register("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("promoter@example.com", "password123", "Promoter");

        // A DRAFT band, created directly (not via an event) - should show up unfiltered
        // and when filtering by status=DRAFT.
        mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Draftband"))))
                .andExpect(status().isCreated());

        // Creating an event with a brand-new band/location produces STUBs - exactly the
        // "still needs completing" data the admin overview needs to surface.
        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(3).toString(),
                "location", Map.of("name", "Stub Venue", "city", "Bremen"),
                "bands", java.util.List.of(Map.of("name", "Stub Band", "city", "Bremen"))
        );
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated());

        // Unfiltered admin band list contains both the DRAFT and the STUB band -
        // something the public /api/bands (PUBLISHED-only) would never show.
        // Sorted by name ascending: "Draftband" before "Stub Band".
        mockMvc.perform(get("/api/admin/bands").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Draftband"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.content[1].name").value("Stub Band"))
                .andExpect(jsonPath("$.content[1].status").value("STUB"));

        mockMvc.perform(get("/api/admin/bands").header("Authorization", "Bearer " + adminToken).param("status", "STUB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Stub Band"))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/admin/locations").header("Authorization", "Bearer " + adminToken).param("status", "STUB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Stub Venue"));

        mockMvc.perform(get("/api/admin/events").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].locationName").value("Stub Venue"))
                .andExpect(jsonPath("$.content[0].bandNames[0]").value("Stub Band"));

        // Non-admins must not reach any of these.
        mockMvc.perform(get("/api/admin/bands").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private String register(String email, String password, String displayName) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "displayName", displayName))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
