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
 * The GPT-skill review queue: the skill (authenticated only via the shared
 * GIGLISTER_GPT_SKILL_TOKEN secret, see GptSkillAuthFilter) can only ever
 * submit a proposal - nothing is created until an admin approves it, and
 * the skill itself can't reach any admin/approve/reject endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SubmissionIntegrationTest {

    private static final String SKILL_TOKEN = "test-only-gpt-skill-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void skillCanOnlySubmitNeverReachAdminOrCreateDirectly() throws Exception {
        String adminToken = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("regular@example.com", "password123", "Regular");

        Map<String, Object> bandSubmission = Map.of(
                "type", "BAND",
                "payload", Map.of("name", "Submitted Band", "city", "Hamburg")
        );

        // No auth at all.
        mockMvc.perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bandSubmission)))
                .andExpect(status().isForbidden());

        // A normal logged-in user's JWT doesn't count either - only the skill secret does.
        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bandSubmission)))
                .andExpect(status().isForbidden());

        // The skill itself can't reach the admin review endpoints.
        mockMvc.perform(get("/api/admin/submissions").header("Authorization", "Bearer " + SKILL_TOKEN))
                .andExpect(status().isForbidden());

        // The correct secret works, and lands as PENDING - nothing created yet.
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bandSubmission)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("BAND"))
                .andExpect(jsonPath("$.payload.name").value("Submitted Band"))
                .andReturn();
        long submissionId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/bands").param("city", "Hamburg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // Admin sees it pending, then approves - only now does the band actually exist.
        mockMvc.perform(get("/api/admin/submissions").header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(submissionId));

        var approveResult = mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.resultEntityId").isNumber())
                .andReturn();
        long bandId = objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("resultEntityId").asLong();

        mockMvc.perform(get("/api/bands/" + bandId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Submitted Band"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Approving twice is a conflict, not a double-create.
        mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectingASubmissionCreatesNothing() throws Exception {
        String adminToken = registerAsAdmin("admin2@giglister.test", "adminpass123", "Admin2");

        Map<String, Object> submission = Map.of(
                "type", "LOCATION",
                "payload", Map.of("name", "Rejected Venue", "city", "Berlin",
                        "address", "Teststraße 1", "postalCode", "10115")
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/submissions/" + id + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Doppelt vorhanden"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Doppelt vorhanden"))
                .andExpect(jsonPath("$.resultEntityId").doesNotExist());

        mockMvc.perform(get("/api/locations").param("city", "Berlin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void approvingAnIncompleteLocationSubmissionFails() throws Exception {
        String adminToken = registerAsAdmin("admin3@giglister.test", "adminpass123", "Admin3");

        // Missing address/postalCode - same requirement as the normal create-event flow.
        Map<String, Object> submission = Map.of(
                "type", "LOCATION",
                "payload", Map.of("name", "Incomplete Venue")
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/submissions/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imageUrlPointingAtAnInternalAddressIsRejectedOnApproval() throws Exception {
        String adminToken = registerAsAdmin("admin4@giglister.test", "adminpass123", "Admin4");

        Map<String, Object> submission = Map.of(
                "type", "BAND",
                "payload", Map.of("name", "SSRF Test Band", "city", "Hamburg"),
                "imageUrl", "http://127.0.0.1/some-image.png"
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/submissions/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        // Rejected fetch must not have created the band either - approval is all-or-nothing.
        mockMvc.perform(get("/api/admin/submissions/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void anAdminCanCorrectASubmissionBeforeApprovingIt() throws Exception {
        String adminToken = registerAsAdmin("admin7@giglister.test", "adminpass123", "Admin7");

        // The skill got the city wrong and forgot a real payload for the SSRF-flagged image - the admin
        // fixes both before approving, rather than having to reject and wait for a resubmission.
        Map<String, Object> submission = Map.of(
                "type", "BAND",
                "payload", Map.of("name", "Typo Band", "city", "Hamurg"),
                "imageUrl", "http://127.0.0.1/blocked.png"
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/submissions/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payload", Map.of("name", "Fixed Band", "city", "Hamburg")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.city").value("Hamburg"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        var approveResult = mockMvc.perform(post("/api/admin/submissions/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();
        long bandId = objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("resultEntityId").asLong();

        mockMvc.perform(get("/api/bands/" + bandId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fixed Band"))
                .andExpect(jsonPath("$.city").value("Hamburg"));
    }

    @Test
    void editingAnAlreadyDecidedSubmissionFails() throws Exception {
        String adminToken = registerAsAdmin("admin8@giglister.test", "adminpass123", "Admin8");

        Map<String, Object> submission = Map.of(
                "type", "BAND",
                "payload", Map.of("name", "Decided Band", "city", "Hamburg")
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/submissions/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/submissions/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payload", Map.of("name", "Too Late", "city", "Hamburg")))))
                .andExpect(status().isConflict());
    }

    @Test
    void approvingAnEventSubmissionCreatesTheInlineLocationAndBand() throws Exception {
        String adminToken = registerAsAdmin("admin5@giglister.test", "adminpass123", "Admin5");

        Map<String, Object> submission = Map.of(
                "type", "EVENT",
                "payload", Map.of(
                        "date", "2027-03-15",
                        "location", Map.of("name", "Submission Venue", "city", "Leipzig",
                                "address", "Teststraße 9", "postalCode", "04109"),
                        "bands", java.util.List.of(Map.of("name", "Submission Band", "city", "Leipzig"))
                )
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        var approveResult = mockMvc.perform(post("/api/admin/submissions/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();
        long eventId = objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("resultEntityId").asLong();

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.name").value("Submission Venue"))
                .andExpect(jsonPath("$.bands[0].name").value("Submission Band"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void approvingAnEventSubmissionWithoutAnAddressOnANewLocationFails() throws Exception {
        String adminToken = registerAsAdmin("admin6@giglister.test", "adminpass123", "Admin6");

        // Same address/postalCode requirement as the normal /api/events endpoint.
        Map<String, Object> submission = Map.of(
                "type", "EVENT",
                "payload", Map.of(
                        "date", "2027-03-15",
                        "location", Map.of("name", "No Address Venue", "city", "Leipzig"),
                        "bands", java.util.List.of(Map.of("name", "Some Band"))
                )
        );
        var submitResult = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + SKILL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/submissions/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Promotes directly via the repository rather than the bootstrap-email/first-user
     * mechanism, since only one account can ever claim that within a shared test
     * context and this class needs an independent admin per test method.
     */
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
