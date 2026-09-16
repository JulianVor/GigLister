package com.giglister.dto.common;

import com.giglister.domain.enums.EntityStatus;

import java.time.LocalTime;
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
        List<String> genres,
        /** This band's own start time within the event it's listed under, distinct from
         * the event's own overall startTime - null means it shares the event's overall
         * time. Only meaningful in the context of a specific event's line-up (e.g. always
         * null on a Band's own profile-page data); see Event.BandLineupEntry. */
        LocalTime startTime
) {
}
