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
 * The month calendar's day counts (§16) must match what the "Konzerte in diesem
 * Monat" list below it actually shows once a device-location radius (§13) is
 * active - otherwise the grid and the list would disagree about the same month.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CalendarLocationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void calendarCountsRespectTheSameRadiusAsTheEventList() throws Exception {
        String token = register("promoter@example.com", "password123", "Promoter");

        LocalDate day = LocalDate.now().withDayOfMonth(15);
        // Hamburg Hafenklang - within a 25km radius of the Hamburg center used below.
        createPublishedEvent(token, day, "Hafenklang", "Hamburg", 53.546, 9.965, "Nearby Band");
        // Berlin SO36 - about 250km away, must be excluded by the radius.
        createPublishedEvent(token, day, "SO36", "Berlin", 52.501, 13.426, "Far Away Band");

        // Unfiltered: both events count.
        mockMvc.perform(get("/api/events/calendar").param("year", String.valueOf(day.getYear())).param("month", String.valueOf(day.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.date == '" + day + "')].count").value(2));

        // Within 25km of Hamburg's center: only the nearby event counts.
        mockMvc.perform(get("/api/events/calendar")
                        .param("year", String.valueOf(day.getYear()))
                        .param("month", String.valueOf(day.getMonthValue()))
                        .param("lat", "53.55")
                        .param("lon", "9.99")
                        .param("radiusKm", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.date == '" + day + "')].count").value(1));
    }

    /**
     * A typed "Standort" (e.g. "Hamburg") geocodes to real coordinates and is sent
     * alongside the plain city string - a venue entered under a differently-spelled but
     * still-nearby city (a district name, a typo, different capitalization/whitespace)
     * must not be hidden just because that exact string doesn't match what was typed. The
     * geocoded radius is the authoritative filter once given (see EventService.
     * filterByLocationRadius); the city string is only a fallback for when no radius could
     * be resolved at all.
     */
    @Test
    void aRealCoordinateRadiusOverridesAMismatchedCityStringInsteadOfAndingWithIt() throws Exception {
        String token = register("radiususer@example.com", "password123", "RadiusUser");

        LocalDate day = LocalDate.now().withDayOfMonth(20);
        // Same city string as what's searched for below.
        createPublishedEvent(token, day, "Molotow", "Hamburg", 53.546, 9.965, "Exact Match Band");
        // A real, nearby (within 25km) Hamburg venue, but entered under a different city string.
        createPublishedEvent(token, day, "Fabrik", "Hamburg-Altona", 53.545, 9.935, "Different City String Band");
        // Genuinely far away - must still be excluded by the radius regardless of its city string.
        createPublishedEvent(token, day, "SO36", "Hamburg", 52.501, 13.426, "Wrong City Despite Matching String");

        mockMvc.perform(get("/api/events").param("city", "Hamburg").param("lat", "53.55").param("lon", "9.99").param("radiusKm", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].location.name", org.hamcrest.Matchers.hasItem("Molotow")))
                .andExpect(jsonPath("$.content[*].location.name", org.hamcrest.Matchers.hasItem("Fabrik")))
                .andExpect(jsonPath("$.content[*].location.name", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("SO36"))));
    }

    private void createPublishedEvent(String token, LocalDate date, String locationName, String city,
                                       double lat, double lon, String bandName) throws Exception {
        var locResult = mockMvc.perform(post("/api/locations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", locationName, "city", city, "latitude", lat, "longitude", lon))))
                .andExpect(status().isCreated())
                .andReturn();
        long locId = objectMapper.readTree(locResult.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch("/api/locations/" + locId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "date", date.toString(),
                                "location", Map.of("id", locId),
                                "bands", java.util.List.of(Map.of("name", bandName))))))
                .andExpect(status().isCreated());
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
