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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A freshly created band/location starts DRAFT (see BandService.create) and stays that
 * way until someone publishes it or a submission auto-completes it - same as
 * BandController/LocationController.assertVisible, search should still be able to lead a
 * logged-in user to it (otherwise a band you just created is unfindable by anyone,
 * creator included, until it's published), while keeping it out of anonymous results.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void aDraftBandIsFindableByALoggedInUserButNotAnonymously() throws Exception {
        String token = register("bandowner@example.com", "password123", "BandOwner");

        mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Silent Echoes", "city", "Berlin"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/search").param("q", "Silent Echoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bands").isEmpty());

        mockMvc.perform(get("/api/search").param("q", "Silent Echoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bands[0].name").value("Silent Echoes"))
                .andExpect(jsonPath("$.bands[0].status").value("DRAFT"));
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
