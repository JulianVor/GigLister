package com.giglister.dto;

import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.common.EventSummary;

import java.util.List;

public record MeResponse(
        Long id,
        String email,
        String username,
        String homeCity,
        Double homeLatitude,
        Double homeLongitude,
        Integer radiusKm,
        boolean platformAdmin,
        List<EventSummary> savedEvents,
        List<ManagedFollowedBand> followedBands,
        List<ManagedEntity> managedEntities
) {
    public record ManagedFollowedBand(Long id, String name, String nextEventDate) {
    }

    public record ManagedEntity(EntityType entityType, Long entityId, String name, PermissionLevel permission) {
    }
}
