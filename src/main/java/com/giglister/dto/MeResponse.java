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
        List<String> preferredGenres,
        boolean platformAdmin,
        /** True right after an admin creates this account with a temporary password -
         * the frontend blocks every page behind a forced "Passwort ändern" screen while
         * this is set (see RequirePasswordChange). */
        boolean mustChangePassword,
        List<EventSummary> savedEvents,
        /** Individual festival acts (band-within-event) this user has gemerkt - see
         * UserService.saveAct. Every one of these implies its eventId is also in
         * savedEvents; the reverse isn't true (a plain whole-event save has no entries
         * here). */
        List<SavedAct> savedActs,
        List<ManagedFollowedBand> followedBands,
        List<ManagedEntity> managedEntities
) {
    public record SavedAct(Long eventId, Long bandId) {
    }

    public record ManagedFollowedBand(
            Long id, String name, String logoUrl, String profileImageUrl, boolean hasActiveStory, String nextEventDate
    ) {
    }

    public record ManagedEntity(EntityType entityType, Long entityId, String name, PermissionLevel permission) {
    }
}
