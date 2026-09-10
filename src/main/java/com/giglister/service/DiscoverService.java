package com.giglister.service;

import com.giglister.domain.Event;
import com.giglister.domain.enums.EventStatus;
import com.giglister.dto.DiscoverResponse;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Entdecken": no recommendation algorithm needed - editorial-feeling sections
 * assembled purely from what is already in the data.
 */
@Service
@RequiredArgsConstructor
public class DiscoverService {

    private static final int SECTION_LIMIT = 10;

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final LocationService locationService;
    private final BandService bandService;
    private final SummaryMapper summaryMapper;

    public DiscoverResponse discover(String city, Double lat, Double lon, Integer radiusKm) {
        LocalDate today = LocalDate.now();
        LocalDate saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDate sunday = saturday.plusDays(1);

        List<EventSummary> todayNearby = eventService
                .filterByLocationRadius(eventRepository.findByStatusAndDateOrderByStartTimeAsc(EventStatus.PUBLISHED, today), city, lat, lon, radiusKm)
                .stream().map(summaryMapper::eventSummary).limit(SECTION_LIMIT).toList();

        List<Event> weekendRaw = new java.util.ArrayList<>();
        weekendRaw.addAll(eventRepository.findByStatusAndDateOrderByStartTimeAsc(EventStatus.PUBLISHED, saturday));
        weekendRaw.addAll(eventRepository.findByStatusAndDateOrderByStartTimeAsc(EventStatus.PUBLISHED, sunday));
        List<EventSummary> thisWeekend = eventService.filterByLocationRadius(weekendRaw, city, lat, lon, radiusKm)
                .stream().map(summaryMapper::eventSummary).limit(SECTION_LIMIT).toList();

        List<Event> upcoming = eventRepository
                .findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(EventStatus.PUBLISHED, today, PageRequest.of(0, 200))
                .getContent();
        List<EventSummary> newEvents = eventService.filterByLocationRadius(upcoming, city, lat, lon, radiusKm).stream()
                .sorted(Comparator.comparing(Event::getCreatedAt).reversed())
                .limit(SECTION_LIMIT)
                .map(summaryMapper::eventSummary)
                .toList();

        var locationsWithUpcoming = locationService.listAllPublishedSortedByUpcoming(city, SECTION_LIMIT).stream()
                .map(locationService::toListItem)
                .toList();

        Set<Long> upcomingBandIds = new LinkedHashSet<>();
        for (Event e : upcoming) {
            upcomingBandIds.addAll(e.getBandIds());
        }
        List<BandResponse> bandsPlayingSoon = bandService.findByIds(List.copyOf(upcomingBandIds)).stream()
                .filter(b -> b.getStatus() == com.giglister.domain.enums.EntityStatus.PUBLISHED)
                .limit(SECTION_LIMIT)
                .map(bandService::toResponse)
                .toList();

        return new DiscoverResponse(todayNearby, thisWeekend, newEvents, locationsWithUpcoming, bandsPlayingSoon);
    }
}
