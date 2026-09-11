package com.giglister.dto.location;

import jakarta.validation.constraints.NotBlank;

public record LocationCreateRequest(
        @NotBlank String name,
        @NotBlank String city,
        String address,
        String postalCode,
        String country,
        String website,
        String logoUrl,
        String titleImageUrl,
        Double latitude,
        Double longitude
) {
}
