package com.giglister.dto.band;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BandUpdateRequest(
        @NotBlank String name,
        String city,
        String region,
        String country,
        String shortDescription,
        String website,
        String logoUrl,
        String titleImageUrl,
        List<String> genres
) {
}
