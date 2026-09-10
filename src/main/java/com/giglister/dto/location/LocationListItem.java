package com.giglister.dto.location;

public record LocationListItem(
        Long id,
        String name,
        String city,
        long upcomingEventCount
) {
}
