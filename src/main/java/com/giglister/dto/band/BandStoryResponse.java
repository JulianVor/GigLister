package com.giglister.dto.band;

import java.time.Instant;

public record BandStoryResponse(
        Long id,
        String imageUrl,
        String text,
        Instant createdAt,
        Instant expiresAt
) {
}
