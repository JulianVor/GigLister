package com.giglister.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Resolves a Location's address to real coordinates via OpenStreetMap's free Nominatim
 * search, so the Orte map (which can only place a marker where it has one) doesn't stay
 * empty just because whoever created the location didn't know its GPS position off-hand.
 * Runs server-side (unlike the frontend's own client-side geocoding for a typed "Standort
 * wählen" city) because the result gets persisted once and reused forever - a single call
 * per location save, not a per-visit lookup, which comfortably fits Nominatim's usage
 * policy as long as it identifies the app via User-Agent (required) and isn't called in a
 * tight bulk loop (LocationAdminService's backfill spaces its own calls out for exactly
 * that reason).
 *
 * Best-effort throughout: geocoding failure (no match, timeout, Nominatim unreachable)
 * never blocks saving a Location - it just leaves that one without a map marker, same as
 * if its address had been left blank. Disabled during tests (giglister.geocoding.enabled)
 * so the suite never depends on outbound network access.
 */
@Service
@Slf4j
public class GeocodingService {

    private final boolean enabled;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GeocodingService(@Value("${giglister.geocoding.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public record Coordinates(double latitude, double longitude) {
    }

    public Optional<Coordinates> geocode(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "GigLister/1.0 (concert listing web app)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode results = mapper.readTree(response.body());
            if (!results.isArray() || results.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = results.get(0);
            return Optional.of(new Coordinates(first.get("lat").asDouble(), first.get("lon").asDouble()));
        } catch (Exception e) {
            log.warn("Geocoding failed for query '{}': {}", query, e.toString());
            return Optional.empty();
        }
    }
}
