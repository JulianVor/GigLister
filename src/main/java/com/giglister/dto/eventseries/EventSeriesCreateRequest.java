package com.giglister.dto.eventseries;

import com.giglister.domain.enums.TimetableStyle;
import jakarta.validation.constraints.NotBlank;

public record EventSeriesCreateRequest(
        @NotBlank String name,
        String description,
        String titleImageUrl,
        String ticketUrl,
        /** Defaults to LIST when omitted. */
        TimetableStyle timetableStyle
) {
}
