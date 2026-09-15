package com.giglister.dto;

import java.util.List;

public record ProfileUpdateRequest(
        String username,
        String homeCity,
        Double homeLatitude,
        Double homeLongitude,
        Integer radiusKm,
        /** Null means "don't touch" (like every other field here); an empty list clears it. */
        List<String> preferredGenres
) {
}
