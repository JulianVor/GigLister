package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityType;

/** A likely-duplicate pair surfaced to admins, distinct from {@link DuplicateCandidate} which
 *  only carries one side (used for the create-time "Meintest du?" suggestions). Merging needs
 *  both ids, so admins get the pair. */
public record DuplicatePair(
        EntityType entityType,
        Long firstId,
        String firstName,
        Long secondId,
        String secondName,
        String city,
        double similarity
) {
}
