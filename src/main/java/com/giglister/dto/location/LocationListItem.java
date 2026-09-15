package com.giglister.dto.location;

import com.giglister.domain.enums.EntityStatus;

public record LocationListItem(
        Long id,
        String name,
        String city,
        long upcomingEventCount,
        EntityStatus status
) {
}
