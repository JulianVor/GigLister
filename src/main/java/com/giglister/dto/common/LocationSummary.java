package com.giglister.dto.common;

import com.giglister.domain.enums.EntityStatus;

public record LocationSummary(
        Long id,
        String name,
        String city,
        EntityStatus status,
        String titleImageUrl,
        boolean linkable
) {
}
