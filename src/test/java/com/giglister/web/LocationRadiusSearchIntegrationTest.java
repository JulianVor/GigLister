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
 * "Standort verwenden" (concept §13): once the frontend has real device
 * coordinates, /api/locations must filter by actual great-circle distance,
 * not just an exact city-name match - and must never hide a location that
 * has no coordinates of its own (e.g. a STUB created without lat/lng).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocationRadiusSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void locationsAreFilteredByRealDistanceWhenCoordinatesAreGiven() throws Exception {
        String token = register("promoter@example.com", "password123", "Promoter");

        // Hamburg Hafenklang - roughly 53.546, 9.965
        createAndPublishLocation(token, "Hafenklang", "Hamburg", 53.546, 9.965);
        // Berlin SO36 - roughly 52.501, 13.426 - about 250km from Hamburg
        createAndPublishLocation(token, "SO36", "Berlin", 52.501, 13.426);
        // A STUB with no coordinates at all must never be hidden by a radius search.
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "date", "2027-01-01",
                                "location", Map.of("name", "No Coords Venue", "city", "Nowhere", "address", "Teststraße 1", "postalCode", "00000"),
                                "bands", java.util.List.of(Map.of("name", "Some Band"))))))
                .andExpect(status().isCreated());

        // Searching within 25km of Hamburg's center must find Hafenklang, but not
        // the Berlin venue ~250km away.
        mockMvc.perform(get("/api/locations")
                        .param("lat", "53.55")
                        .param("lon", "9.99")
                        .param("radiusKm", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.hasItem("Hafenklang")))
                .andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("SO36"))));

        // Without any location filter, both PUBLISHED locations show up.
        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    private void createAndPublishLocation(String token, String name, String city, double lat, double lon) throws Exception {
        var result = mockMvc.perform(post("/api/locations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "city", city, "latitude", lat, "longitude", lon))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch("/api/locations/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"))))
                .andExpect(status().isOk());
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
