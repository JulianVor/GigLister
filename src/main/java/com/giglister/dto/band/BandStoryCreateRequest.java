package com.giglister.dto.band;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BandStoryCreateRequest(
        @NotBlank String imageUrl,
        @Size(max = 280) String text
) {
}
