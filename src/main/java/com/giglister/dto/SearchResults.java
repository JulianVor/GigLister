package com.giglister.dto;

import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.dto.location.LocationListItem;

import java.util.List;

public record SearchResults(
        List<EventSummary> events,
        List<BandResponse> bands,
        List<LocationListItem> locations
) {
}
