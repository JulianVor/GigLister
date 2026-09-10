package com.giglister.dto.band;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BandCreateRequest(
        @NotBlank String name,
        String city,
        String region,
        String country,
        String shortDescription,
        String website,
        List<String> genres
) {
}
