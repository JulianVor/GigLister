package com.giglister.dto.admin;

import com.giglister.domain.enums.ClaimStatus;
import com.giglister.domain.enums.EntityType;

import java.time.Instant;

public record ClaimResponse(
        Long id,
        EntityType entityType,
        Long entityId,
        String entityName,
        Long requestedBy,
        String requestedByEmail,
        String message,
        ClaimStatus status,
        Instant requestedAt
) {
}
