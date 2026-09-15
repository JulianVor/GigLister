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
 * POST /api/events publishes immediately only for someone with direct create rights
 * (platform admin, or EDIT+ on the referenced location or a referenced band - see
 * EventService.canCreateDirectly); anyone else's proposal is routed into the review
 * queue as a PENDING Submission instead, exactly like a GPT-skill proposal, and only
 * goes live once a platform admin approves it (see EventController.create).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventSubmissionRoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void aRegularUserWithNoPermissionsGetsQueuedInsteadOfPublished() throws Exception {
        String adminToken = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("fan@example.com", "password123", "Fan");

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(7).toString(),
                "location", Map.of("name", "Unbekannter Ort", "city", "Köln", "address", "Teststraße 1", "postalCode", "50667"),
                "bands", List.of(Map.of("name", "Unbekannte Band", "city", "Köln"))
        );

        var result = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.event").doesNotExist())
                .andExpect(jsonPath("$.submission.status").value("PENDING"))
                .andExpect(jsonPath("$.submission.type").value("EVENT"))
                .andExpect(jsonPath("$.submission.submittedByUsername").value("Fan"))
                .andReturn();
        long submissionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("submission").get("id").asLong();

        // Nothing published yet - the location/band never even become real rows.
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // Shows up in the user's own "Meine Vorschläge".
        mockMvc.perform(get("/api/me/submissions").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(submissionId))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Approving it publishes the event, attributed to the ORIGINAL submitter (not the
        // approving admin) - so they keep normal edit/delete rights over what they proposed.
        var approveResult = mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();
        long eventId = objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("resultEntityId").asLong();

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.name").value("Unbekannter Ort"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // The original submitter, not the admin, can edit/delete their now-published event.
        mockMvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void aBandManagerPublishesDirectlyWhenReferencingTheirOwnBandById() throws Exception {
        // Registered before "Manager" purely so Manager can never accidentally be the very
        // first user ever created in a fresh context (which would auto-grant platform admin
        // and mask whether this test is really exercising the band-permission path).
        String strangerToken = register("stranger@example.com", "password123", "Stranger");
        String token = register("manager@example.com", "password123", "Manager");

        var bandResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Die Verwalteten"))))
                .andExpect(status().isCreated())
                .andReturn();
        long bandId = objectMapper.readTree(bandResult.getResponse().getContentAsString()).get("id").asLong();

        // A second, unrelated band the manager does NOT hold any permission on.
        var otherBandResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Die Fremden"))))
                .andExpect(status().isCreated())
                .andReturn();
        long otherBandId = objectMapper.readTree(otherBandResult.getResponse().getContentAsString()).get("id").asLong();

        // References their own band (by id) plus the other one - still publishes directly,
        // "mit ihrer plus weitere Bands".
        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(7).toString(),
                "location", Map.of("name", "Live-Halle", "city", "Essen", "address", "Teststraße 2", "postalCode", "45127"),
                "bands", List.of(Map.of("id", bandId), Map.of("id", otherBandId))
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.submission").doesNotExist())
                .andExpect(jsonPath("$.event.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.event.bands.length()").value(2));
    }

    @Test
    void referencingAnExistingBandByNameInsteadOfIdStillGetsQueued() throws Exception {
        // Naming an existing band without its id looks the same as proposing a brand-new one
        // from the API's point of view - EventService.resolveBand only tries a name+city match
        // *inside* create(), which the routing check never reaches for a non-manager.
        // Manager2 registers first purely so Fan2 can never accidentally be the very first
        // user ever created in a fresh context (which would auto-grant platform admin).
        String managerToken = register("manager2@example.com", "password123", "Manager2");
        String token = register("fan2@example.com", "password123", "Fan2");
        mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Namensband", "city", "Mainz"))))
                .andExpect(status().isCreated());

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(7).toString(),
                "location", Map.of("name", "Andere Halle", "city", "Mainz", "address", "Teststraße 3", "postalCode", "55116"),
                "bands", List.of(Map.of("name", "Namensband", "city", "Mainz"))
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.submission.status").value("PENDING"));
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
