package com.giglister.service;

import com.giglister.domain.EventSeries;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.domain.enums.TimetableStyle;
import com.giglister.dto.common.EventSeriesSummary;
import com.giglister.dto.eventseries.EventSeriesCreateRequest;
import com.giglister.dto.eventseries.EventSeriesResponse;
import com.giglister.dto.eventseries.EventSeriesUpdateRequest;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.EventRepository;
import com.giglister.repository.EventSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSeriesService {

    private final EventSeriesRepository eventSeriesRepository;
    private final EventRepository eventRepository;
    private final PermissionService permissionService;
    private final SummaryMapper summaryMapper;

    public EventSeries getOrThrow(Long id) {
        return eventSeriesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("EventSeries " + id + " not found"));
    }

    /** Every series, name-ascending - the list a "which Reihe does this belong to"
     * dropdown on the event form picks from, so there's no separate published/draft
     * split to filter by (see EventSeries' own class comment). */
    public List<EventSeriesSummary> list() {
        return eventSeriesRepository.findAll().stream()
                .sorted(Comparator.comparing(EventSeries::getName, String.CASE_INSENSITIVE_ORDER))
                .map(s -> new EventSeriesSummary(s.getId(), s.getName(), s.getTitleImageUrl(), s.getTicketUrl()))
                .toList();
    }

    @Transactional
    public EventSeries create(EventSeriesCreateRequest request, Long createdBy) {
        EventSeries series = EventSeries.builder()
                .name(request.name())
                .description(request.description())
                .titleImageUrl(request.titleImageUrl())
                .ticketUrl(request.ticketUrl())
                .timetableStyle(request.timetableStyle() != null ? request.timetableStyle() : TimetableStyle.LIST)
                .createdBy(createdBy)
                .build();
        series = eventSeriesRepository.save(series);
        permissionService.grant(EntityType.EVENT_SERIES, series.getId(), createdBy, PermissionLevel.MANAGE, createdBy);
        return series;
    }

    @Transactional
    public EventSeries update(Long id, EventSeriesUpdateRequest request, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.EVENT_SERIES, id, PermissionLevel.EDIT);
        EventSeries series = getOrThrow(id);
        series.setName(request.name());
        series.setDescription(request.description());
        series.setTitleImageUrl(request.titleImageUrl());
        series.setTicketUrl(request.ticketUrl());
        series.setTimetableStyle(request.timetableStyle() != null ? request.timetableStyle() : TimetableStyle.LIST);
        return eventSeriesRepository.save(series);
    }

    public EventSeriesResponse toResponse(EventSeries series) {
        var events = eventRepository.findByEventSeriesIdOrderByDateAscStartTimeAsc(series.getId()).stream()
                .map(summaryMapper::eventSummary)
                .toList();
        return new EventSeriesResponse(series.getId(), series.getName(), series.getDescription(),
                series.getTitleImageUrl(), series.getTicketUrl(), series.getTimetableStyle(), events);
    }
}
