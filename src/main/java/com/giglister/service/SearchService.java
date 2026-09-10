package com.giglister.service;

import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EventStatus;
import com.giglister.dto.SearchResults;
import com.giglister.repository.BandRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_RESULTS_PER_TYPE = 20;

    private final EventRepository eventRepository;
    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;
    private final BandService bandService;
    private final LocationService locationService;
    private final SummaryMapper summaryMapper;

    public SearchResults search(String query, String type) {
        boolean wantEvents = type == null || type.equalsIgnoreCase("ALL") || type.equalsIgnoreCase("EVENT");
        boolean wantBands = type == null || type.equalsIgnoreCase("ALL") || type.equalsIgnoreCase("BAND");
        boolean wantLocations = type == null || type.equalsIgnoreCase("ALL") || type.equalsIgnoreCase("LOCATION");

        var events = wantEvents
                ? eventRepository.searchByTitleAndStatus(query, EventStatus.PUBLISHED, LocalDate.now(), LocalDate.now().plusYears(2))
                .stream().limit(MAX_RESULTS_PER_TYPE).map(summaryMapper::eventSummary).toList()
                : List.<com.giglister.dto.common.EventSummary>of();

        var bands = wantBands
                ? bandRepository.searchByNameAndStatus(query, EntityStatus.PUBLISHED)
                .stream().limit(MAX_RESULTS_PER_TYPE).map(bandService::toResponse).toList()
                : List.<com.giglister.dto.band.BandResponse>of();

        var locations = wantLocations
                ? locationRepository.searchByNameAndStatus(query, EntityStatus.PUBLISHED)
                .stream().limit(MAX_RESULTS_PER_TYPE).map(locationService::toListItem).toList()
                : List.<com.giglister.dto.location.LocationListItem>of();

        return new SearchResults(events, bands, locations);
    }
}
