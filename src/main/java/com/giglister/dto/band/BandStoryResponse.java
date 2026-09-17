package com.giglister.dto.band;

import java.time.Instant;

public record BandStoryResponse(
        Long id,
        String imageUrl,
        String text,
        Double imgWidthPct,
        Double imgHeightPct,
        Double imgOffsetLeftPct,
        Double imgOffsetTopPct,
        Instant createdAt,
        Instant expiresAt
) {
}
