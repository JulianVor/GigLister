package com.giglister.dto.band;

import com.giglister.domain.enums.EntityStatus;
import com.giglister.dto.common.EventSummary;

import java.util.List;

public record BandResponse(
        Long id,
        String name,
        String city,
        String region,
        String country,
        String shortDescription,
        String website,
        String logoUrl,
        String titleImageUrl,
        List<String> genres,
        EntityStatus status,
        boolean unclaimed,
        List<EventSummary> upcomingEvents
) {
}
