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
 * "Alle Daten exportieren/importieren" (see DataTransferService) - covers everything the
 * H2 test database can portably exercise: the export's shape/content, admin-only access,
 * and that a missing/wrong confirmation phrase changes nothing. importAll's actual
 * TRUNCATE ... RESTART IDENTITY CASCADE is Postgres-specific syntax H2 doesn't parse the
 * same way (same situation as EnumCheckConstraintSync/GenreRenameMigration - neither has
 * H2 test coverage either) - the real wipe-and-restore round trip (ids preserved,
 * sequences resynced) is verified by hand against the local disposable Postgres instead,
 * never against a live database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminDataTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void exportReturnsEverythingAndImportRequiresTheExactConfirmationPhrase() throws Exception {
        String adminToken = registerAsAdmin("admin@giglister.test", "adminpass123", "Admin");
        String userToken = register("fan@example.com", "password123", "Fan");

        Map<String, Object> eventRequest = Map.of(
                "date", LocalDate.now().plusDays(10).toString(),
                "location", Map.of("name", "Export Venue", "city", "Bremen", "address", "Teststraße 5", "postalCode", "28195"),
                "bands", List.of(Map.of("name", "Export Band", "city", "Bremen"))
        );
        var eventResult = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        var eventJson = objectMapper.readTree(eventResult.getResponse().getContentAsString()).get("event");
        long eventId = eventJson.get("id").asLong();
        long bandId = eventJson.get("bands").get(0).get("id").asLong();

        mockMvc.perform(post("/api/bands/" + bandId + "/follow").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/events/" + eventId + "/save").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Non-admins can't reach either endpoint.
        mockMvc.perform(get("/api/admin/data/export").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/data/import").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        var exportResult = mockMvc.perform(get("/api/admin/data/export").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(jsonPath("$.bands.length()").value(1))
                .andExpect(jsonPath("$.bands[0].id").value(bandId))
                .andExpect(jsonPath("$.bands[0].name").value("Export Band"))
                .andExpect(jsonPath("$.locations.length()").value(1))
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].bandLineup[0].bandId").value(bandId))
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.bandFollows.length()").value(1))
                .andExpect(jsonPath("$.bandFollows[0].bandId").value(bandId))
                .andExpect(jsonPath("$.savedEvents.length()").value(1))
                .andExpect(jsonPath("$.savedEvents[0].eventId").value(eventId))
                .andReturn();
        var exported = objectMapper.readTree(exportResult.getResponse().getContentAsString());

        // A missing/wrong confirmation phrase is rejected before anything is touched -
        // never a partial or best-effort import.
        mockMvc.perform(post("/api/admin/data/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("data", exported))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/data/import")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirm", "yes please", "data", exported))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/bands").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

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
