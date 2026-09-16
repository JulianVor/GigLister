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
 * Each band in an event's line-up can optionally have its own start time, distinct from
 * the event's overall startTime - e.g. a festival day where each band goes on stage at a
 * different time (see Event.BandLineupEntry / EntityRef.startTime).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BandStartTimeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void eachBandCanHaveItsOwnOptionalStartTimeWithinTheEventLineup() throws Exception {
        String token = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(30).toString(),
                "startTime", "18:00:00",
                "location", Map.of("name", "Festhalle", "city", "Köln", "address", "Teststraße 1", "postalCode", "50667"),
                "bands", List.of(
                        Map.of("name", "Opener Band", "startTime", "18:30:00"),
                        Map.of("name", "Headliner Band", "startTime", "20:00:00"),
                        Map.of("name", "No Time Band")
                )
        );

        var createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.event.bands[0].name").value("Opener Band"))
                .andExpect(jsonPath("$.event.bands[0].startTime").value("18:30:00"))
                .andExpect(jsonPath("$.event.bands[1].name").value("Headliner Band"))
                .andExpect(jsonPath("$.event.bands[1].startTime").value("20:00:00"))
                .andExpect(jsonPath("$.event.bands[2].name").value("No Time Band"))
                .andExpect(jsonPath("$.event.bands[2].startTime").doesNotExist())
                .andReturn();
        long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("event").get("id").asLong();

        // Reading it back (a fresh request, detached from the create transaction) must
        // show the same per-band times.
        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bands[0].startTime").value("18:30:00"))
                .andExpect(jsonPath("$.bands[1].startTime").value("20:00:00"))
                .andExpect(jsonPath("$.bands[2].startTime").doesNotExist());

        // Editing can change/remove per-band times independently of the event's own startTime.
        long openerId = objectMapper.readTree(
                mockMvc.perform(get("/api/events/" + eventId)).andReturn().getResponse().getContentAsString())
                .get("bands").get(0).get("id").asLong();
        long headlinerId = objectMapper.readTree(
                mockMvc.perform(get("/api/events/" + eventId)).andReturn().getResponse().getContentAsString())
                .get("bands").get(1).get("id").asLong();

        Map<String, Object> updateRequest = Map.of(
                "date", LocalDate.now().plusDays(30).toString(),
                "startTime", "18:00:00",
                "location", Map.of("name", "Festhalle", "city", "Köln", "address", "Teststraße 1", "postalCode", "50667"),
                "bands", List.of(
                        Map.of("id", openerId), // time removed
                        Map.of("id", headlinerId, "startTime", "20:30:00") // time changed
                )
        );
        mockMvc.perform(put("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bands[0].startTime").doesNotExist())
                .andExpect(jsonPath("$.bands[1].startTime").value("20:30:00"));
    }

    /** Promotes directly via the repository rather than the bootstrap-email/first-user
     * mechanism, since only one account can ever claim that within a shared test context. */
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
