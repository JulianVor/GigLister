package com.giglister.dto.eventseries;

import jakarta.validation.constraints.NotBlank;

public record EventSeriesUpdateRequest(
        @NotBlank String name,
        String description,
        String titleImageUrl,
        String ticketUrl
) {
}
