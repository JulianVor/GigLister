package com.giglister.dto.common;

import com.giglister.domain.enums.EntityStatus;

public record BandSummary(
        Long id,
        String name,
        String city,
        EntityStatus status,
        String logoUrl,
        String titleImageUrl,
        boolean linkable
) {
}
