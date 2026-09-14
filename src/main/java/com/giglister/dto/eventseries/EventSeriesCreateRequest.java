package com.giglister.dto.eventseries;

import jakarta.validation.constraints.NotBlank;

public record EventSeriesCreateRequest(
        @NotBlank String name,
        String description,
        String titleImageUrl,
        String ticketUrl
) {
}
