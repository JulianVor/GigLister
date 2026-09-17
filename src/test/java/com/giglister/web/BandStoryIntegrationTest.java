package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giglister.domain.BandStory;
import com.giglister.repository.BandStoryRepository;
import com.giglister.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A band's 24h-expiring photo+text status (see BandStoryController/BandStoryService), and
 * the profileImageUrl field (dedicated picture, falls back to logoUrl on the frontend -
 * see UserMenu/StoryRing) that ships alongside it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BandStoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BandStoryRepository bandStoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void managerCanPostAndDeleteAStoryOthersCanReadButNotWrite() throws Exception {
        String managerToken = register("storymanager@giglister.test", "managerpass123", "StoryManager");

        var createResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Story Band", "profileImageUrl", "https://example.com/pfp.png"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileImageUrl").value("https://example.com/pfp.png"))
                .andReturn();
        long bandId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Published so the anonymous GET below actually exercises the public-visibility path
        // (a STUB/DRAFT band's stories are hidden from anonymous visitors too - see
        // BandService.assertVisible, shared by BandController and BandStoryService).
        mockMvc.perform(patch("/api/bands/" + bandId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"))))
                .andExpect(status().isOk());

        var storyResult = mockMvc.perform(post("/api/bands/" + bandId + "/stories")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "imageUrl", "https://example.com/story.jpg", "text", "Neues Video ist raus!",
                                "imgWidthPct", 150.0, "imgHeightPct", 100.0, "imgCenterXPct", 60.0, "imgCenterYPct", 50.0,
                                "imgRotationDeg", 12.5, "imgBackgroundColor", "#3a2a1f",
                                "textLayersJson", "[{\"id\":\"t1\",\"text\":\"NEUES VIDEO\",\"centerXPct\":50,\"centerYPct\":20,\"scale\":1.5,\"rotationDeg\":0}]",
                                "bandTagsJson", "[{\"id\":\"b1\",\"bandId\":42,\"bandName\":\"Support Act\",\"profileImageUrl\":null,\"logoUrl\":null,\"centerXPct\":50,\"centerYPct\":80,\"scale\":1,\"rotationDeg\":0}]"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/story.jpg"))
                .andExpect(jsonPath("$.text").value("Neues Video ist raus!"))
                .andExpect(jsonPath("$.imgWidthPct").value(150.0))
                .andExpect(jsonPath("$.imgHeightPct").value(100.0))
                .andExpect(jsonPath("$.imgCenterXPct").value(60.0))
                .andExpect(jsonPath("$.imgCenterYPct").value(50.0))
                .andExpect(jsonPath("$.imgRotationDeg").value(12.5))
                .andExpect(jsonPath("$.imgBackgroundColor").value("#3a2a1f"))
                .andExpect(jsonPath("$.textLayersJson").value(
                        "[{\"id\":\"t1\",\"text\":\"NEUES VIDEO\",\"centerXPct\":50,\"centerYPct\":20,\"scale\":1.5,\"rotationDeg\":0}]"))
                .andExpect(jsonPath("$.bandTagsJson").value(
                        "[{\"id\":\"b1\",\"bandId\":42,\"bandName\":\"Support Act\",\"profileImageUrl\":null,\"logoUrl\":null,\"centerXPct\":50,\"centerYPct\":80,\"scale\":1,\"rotationDeg\":0}]"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();
        long storyId = objectMapper.readTree(storyResult.getResponse().getContentAsString()).get("id").asLong();

        BandStory saved = bandStoryRepository.findById(storyId).orElseThrow();
        assertThat(saved.getExpiresAt()).isCloseTo(saved.getCreatedAt().plus(24, ChronoUnit.HOURS), within(1, ChronoUnit.SECONDS));

        // No Authorization header at all - the public GET path.
        mockMvc.perform(get("/api/bands/" + bandId + "/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(storyId));

        String otherUserToken = register("notamanager@giglister.test", "otherpass123", "NotAManager");
        mockMvc.perform(post("/api/bands/" + bandId + "/stories")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("imageUrl", "https://example.com/hijack.jpg"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bands/" + bandId + "/stories/" + storyId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/bands/" + bandId + "/stories/" + storyId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bands/" + bandId + "/stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anAlreadyExpiredStoryIsExcludedFromTheActiveListing() throws Exception {
        String managerToken = register("expiredstorymanager@giglister.test", "managerpass123", "ExpiredStoryManager");

        var createResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Expired Story Band"))))
                .andExpect(status().isCreated())
                .andReturn();
        long bandId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        BandStory expired = bandStoryRepository.save(BandStory.builder()
                .bandId(bandId)
                .imageUrl("https://example.com/old.jpg")
                .createdAt(Instant.now().minus(30, ChronoUnit.HOURS))
                .expiresAt(Instant.now().minus(6, ChronoUnit.HOURS))
                .build());

        mockMvc.perform(get("/api/bands/" + bandId + "/stories")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(bandStoryRepository.findById(expired.getId())).isPresent();
    }

    @Test
    void bandSearchFindsPublishedBandsByNameSubstringForTagging() throws Exception {
        String managerToken = register("tagsearchmanager@giglister.test", "managerpass123", "TagSearchManager");

        var createResult = mockMvc.perform(post("/api/bands")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Support Act Supreme", "profileImageUrl", "https://example.com/support.png"))))
                .andExpect(status().isCreated())
                .andReturn();
        long bandId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Unpublished (STUB) - must not show up in tag search results.
        mockMvc.perform(get("/api/bands/search").param("q", "Support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/bands/" + bandId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"))))
                .andExpect(status().isOk());

        // No Authorization header - a band manager searching to tag another band while
        // composing a story hits this anonymously through PUBLIC_API_URL (see
        // BandStoryComposer), same as everything else under GET /api/bands/**.
        mockMvc.perform(get("/api/bands/search").param("q", "support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(bandId))
                .andExpect(jsonPath("$[0].name").value("Support Act Supreme"))
                .andExpect(jsonPath("$[0].profileImageUrl").value("https://example.com/support.png"));

        mockMvc.perform(get("/api/bands/search").param("q", "no-such-band-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
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
