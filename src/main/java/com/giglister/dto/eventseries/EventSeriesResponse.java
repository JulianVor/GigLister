package com.giglister.dto.eventseries;

import com.giglister.dto.common.EventSummary;

import java.util.List;

public record EventSeriesResponse(
        Long id,
        String name,
        String description,
        String titleImageUrl,
        String ticketUrl,
        /** Every Event referencing this series, date-ascending - what the series' own
         * page actually lists. */
        List<EventSummary> events
) {
}
