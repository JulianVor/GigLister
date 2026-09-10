package com.giglister.dto.location;

import com.giglister.domain.enums.EntityStatus;
import com.giglister.dto.common.EventSummary;

import java.util.List;
import java.util.Map;

public record LocationResponse(
        Long id,
        String name,
        String city,
        String address,
        String postalCode,
        String country,
        String website,
        String logoUrl,
        String titleImageUrl,
        Double latitude,
        Double longitude,
        EntityStatus status,
        boolean unclaimed,
        List<EventSummary> upcomingEvents,
        Map<Integer, List<EventSummary>> pastEventsByYear
) {
}
