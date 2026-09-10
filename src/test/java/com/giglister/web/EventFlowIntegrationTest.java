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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerAndCreateEventWithNewBandAndLocationStubs() throws Exception {
        String token = register("anna@example.com", "password123", "Anna");

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

        // Regression check: reading a previously-created event back (detail, and an
        // unfiltered list) must not throw LazyInitializationException on Event.bandIds -
        // this only surfaces once the entity is read back detached from its original
        // transaction, which the assertions above (same-request city filter) don't exercise.
        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bands[0].name").value("HOME"));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + eventId + ")]").exists());
    }

    /** Registers and immediately confirms the email (looking the token up directly,
     * the way the frontend does after the user follows the link), returning a usable token. */
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
