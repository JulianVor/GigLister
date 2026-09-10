package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the core V1 flow end-to-end: register, create an event with inline
 * band/location stubs (section 30-32 of the concept), then find it via the
 * public events list.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndCreateEventWithNewBandAndLocationStubs() throws Exception {
        var registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "anna@example.com",
                                "password", "password123",
                                "displayName", "Anna"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("token").asText();

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(7).toString(),
                "startTime", "20:00:00",
                "location", Map.of("name", "Hafenklang", "city", "Hamburg"),
                "bands", java.util.List.of(Map.of("name", "HOME", "city", "Hamburg"))
        );

        var createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location.name").value("Hafenklang"))
                .andExpect(jsonPath("$.bands[0].name").value("HOME"))
                .andExpect(jsonPath("$.bands[0].linkable").value(false))
                .andReturn();

        String eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/events").param("city", "Hamburg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + eventId + ")]").exists());

        mockMvc.perform(get("/api/bands").param("city", "Hamburg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
