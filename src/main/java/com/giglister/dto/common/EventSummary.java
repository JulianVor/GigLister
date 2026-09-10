package com.giglister.dto.common;

import com.giglister.domain.enums.EventStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventSummary(
        Long id,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocationSummary location,
        List<BandSummary> bands,
        String titleImageUrl,
        EventStatus status
) {
}
