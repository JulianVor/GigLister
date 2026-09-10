package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A STUB Band/Location created inline while adding an event (§31/32) has no
 * MANAGE holder yet. It must stay invisible to anonymous visitors (no "empty
 * profile pages" for the public), but any logged-in user has to be able to
 * reach it - otherwise nobody could ever find and claim it (§37/38).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StubVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void stubBandAndLocationAreHiddenFromAnonymousButReachableByAnyLoggedInUser() throws Exception {
        String creatorToken = register("creator@example.com", "password123", "Creator");
        String bystanderToken = register("bystander@example.com", "password123", "Bystander");

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(5).toString(),
                "location", Map.of("name", "New Venue", "city", "Leipzig"),
                "bands", java.util.List.of(Map.of("name", "New Band", "city", "Leipzig"))
        );
        var createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long bandId = body.get("bands").get(0).get("id").asLong();
        long locationId = body.get("location").get("id").asLong();

        // Anonymous: both are STUB and unclaimed, so they must not be reachable.
        mockMvc.perform(get("/api/bands/" + bandId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/locations/" + locationId)).andExpect(status().isNotFound());

        // The creator (who has no permission grant on either - inline stub
        // creation intentionally doesn't grant one) can still view them.
        mockMvc.perform(get("/api/bands/" + bandId).header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/locations/" + locationId).header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // A completely unrelated logged-in user must be able to reach them too,
        // otherwise the only way to discover and claim a stub is knowing its ID.
        mockMvc.perform(get("/api/bands/" + bandId).header("Authorization", "Bearer " + bystanderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unclaimed").value(true));
        mockMvc.perform(get("/api/locations/" + locationId).header("Authorization", "Bearer " + bystanderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unclaimed").value(true));
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
