package com.giglister.dto.common;

import com.giglister.domain.enums.EntityStatus;

import java.util.List;

public record BandSummary(
        Long id,
        String name,
        String city,
        EntityStatus status,
        String logoUrl,
        String titleImageUrl,
        boolean linkable,
        /** Used by the event card's colored-tile fallback when there's no photo at all. */
        List<String> genres
) {
}
