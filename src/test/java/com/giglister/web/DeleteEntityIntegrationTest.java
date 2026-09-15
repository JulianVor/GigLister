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
 * Covers deleting Events/Bands/Locations: always allowed for an event, but a band or
 * location refuses to delete while any event (of any status) still references it - same
 * "kein Konzert darf seine Location/Band verlieren" rule merge() already enforces via
 * relinking, just as a hard stop here since there's no target to relink onto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeleteEntityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void bandAndLocationCannotBeDeletedWhileAnEventReferencesThemButCanAfterItsGone() throws Exception {
        String token = register("promoter@example.com", "password123", "Promoter");

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(7).toString(),
                "location", Map.of("name", "Hafenklang", "city", "Hamburg", "address", "Große Elbstraße 132", "postalCode", "22767"),
                "bands", List.of(Map.of("name", "HOME", "city", "Hamburg"))
        );
        var createResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var eventJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long eventId = eventJson.get("id").asLong();
        long locationId = eventJson.get("location").get("id").asLong();
        long bandId = eventJson.get("bands").get(0).get("id").asLong();

        mockMvc.perform(delete("/api/bands/" + bandId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/locations/" + locationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // The event itself has nothing blocking it - always deletable by whoever may edit it.
        mockMvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isNotFound());

        // Now that no event references them anymore, both can be deleted.
        mockMvc.perform(delete("/api/bands/" + bandId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bands/" + bandId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/locations/" + locationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/locations/" + locationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingABandStopsItFromShowingUpInAFollowersProfile() throws Exception {
        String ownerToken = register("owner@example.com", "password123", "Owner");
        String followerToken = register("follower@example.com", "password123", "Follower");

        var bandResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Standalone Band"))))
                .andExpect(status().isCreated())
                .andReturn();
        long bandId = objectMapper.readTree(bandResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/bands/" + bandId + "/follow").header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/bands/" + bandId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Would otherwise throw NotFoundException resolving the now-gone band from the
        // follower's BandFollow row - confirms the follow itself got cleaned up, not just
        // tolerated.
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedBands").isEmpty());
    }

    @Test
    void onlyAManagerCanDeleteABandOrLocation() throws Exception {
        String ownerToken = register("bandowner@example.com", "password123", "BandOwner");
        String otherToken = register("stranger@example.com", "password123", "Stranger");

        var bandResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Guarded Band"))))
                .andExpect(status().isCreated())
                .andReturn();
        long bandId = objectMapper.readTree(bandResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/bands/" + bandId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/bands/" + bandId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
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
