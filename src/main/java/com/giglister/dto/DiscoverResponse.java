package com.giglister.dto;

import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.dto.location.LocationListItem;

import java.util.List;

public record DiscoverResponse(
        List<EventSummary> todayNearby,
        List<EventSummary> thisWeekend,
        List<EventSummary> newEvents,
        List<LocationListItem> locationsWithUpcomingShows,
        List<BandResponse> bandsPlayingSoon,
        /** Rule-based, not a real ML recommender - see DiscoverService.recommendedForYou. Empty
         * for a logged-out visitor, or a logged-in one with no signal AND no fallback data. */
        List<EventSummary> recommendedForYou
) {
}
