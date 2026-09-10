package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityType;

public record DuplicateCandidate(
        EntityType entityType,
        Long id,
        String name,
        String city,
        double similarity
) {
}
