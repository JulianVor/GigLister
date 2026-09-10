package com.giglister.dto.event;

import com.giglister.domain.enums.EventStatus;
import com.giglister.dto.common.BandSummary;
import com.giglister.dto.common.LocationSummary;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventResponse(
        Long id,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocationSummary location,
        List<BandSummary> bands,
        String description,
        String ticketUrl,
        String titleImageUrl,
        EventStatus status,
        Long createdBy
) {
}
