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
 * "Meine Bands" / "Meine Veranstaltungen": a promoter managing several bands
 * should see all of them, and every upcoming show across all of them in one
 * band-übergreifend list - deduplicated when two of their bands share a bill.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MyBandsAndEventsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void myBandsAndEventsAggregateAcrossAllManagedBandsWithoutDuplicates() throws Exception {
        String token = register("promoter@example.com", "password123", "Promoter");

        createBand(token, "Band A");
        createBand(token, "Band B");

        // A solo show for Band A.
        createEvent(token, LocalDate.now().plusDays(2), "Band A", null);
        // A shared bill: both of the promoter's bands play the same event.
        createEvent(token, LocalDate.now().plusDays(5), "Band A", "Band B");

        mockMvc.perform(get("/api/me/bands").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Band A"))
                .andExpect(jsonPath("$[1].name").value("Band B"));

        // Two events total, not three - the shared bill must not be duplicated.
        mockMvc.perform(get("/api/me/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private long createBand(String token, String name) throws Exception {
        var result = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void createEvent(String token, LocalDate date, String... bandNames) throws Exception {
        var bands = java.util.Arrays.stream(bandNames)
                .filter(java.util.Objects::nonNull)
                .map(name -> Map.of("name", (Object) name))
                .toList();
        Map<String, Object> eventRequest = Map.of(
                "date", date.toString(),
                "location", Map.of("name", "Venue", "city", "Berlin"),
                "bands", bands
        );
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated());
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
