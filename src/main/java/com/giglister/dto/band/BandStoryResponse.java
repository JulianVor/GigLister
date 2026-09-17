package com.giglister.dto.band;

import java.time.Instant;

public record BandStoryResponse(
        Long id,
        String imageUrl,
        String text,
        Double imgWidthPct,
        Double imgHeightPct,
        Double imgCenterXPct,
        Double imgCenterYPct,
        Double imgRotationDeg,
        String imgBackgroundColor,
        String textLayersJson,
        String bandTagsJson,
        Instant createdAt,
        Instant expiresAt
) {
}
