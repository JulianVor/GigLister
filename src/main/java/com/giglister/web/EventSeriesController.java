package com.giglister.web;

import com.giglister.domain.EventSeries;
import com.giglister.dto.common.EventSeriesSummary;
import com.giglister.dto.eventseries.EventSeriesCreateRequest;
import com.giglister.dto.eventseries.EventSeriesResponse;
import com.giglister.dto.eventseries.EventSeriesUpdateRequest;
import com.giglister.security.CurrentUser;
import com.giglister.service.EventSeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Festivals/themed nights that bundle several separate {@link com.giglister.domain.Event}s
 * together (e.g. "SüdKultur MusicNight") - see EventSeries' own class comment for why
 * there's no draft/published moderation step here, unlike Band/Location.
 */
@RestController
@RequestMapping("/api/event-series")
@RequiredArgsConstructor
public class EventSeriesController {

    private final EventSeriesService eventSeriesService;

    @GetMapping
    public List<EventSeriesSummary> list() {
        return eventSeriesService.list();
    }

    @GetMapping("/{id}")
    public EventSeriesResponse get(@PathVariable Long id) {
        return eventSeriesService.toResponse(eventSeriesService.getOrThrow(id));
    }

    @PostMapping
    public ResponseEntity<EventSeriesResponse> create(@Valid @RequestBody EventSeriesCreateRequest request) {
        EventSeries series = eventSeriesService.create(request, CurrentUser.requireId());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventSeriesService.toResponse(series));
    }

    @PutMapping("/{id}")
    public EventSeriesResponse update(@PathVariable Long id, @Valid @RequestBody EventSeriesUpdateRequest request) {
        var user = CurrentUser.require();
        EventSeries series = eventSeriesService.update(id, request, user.getId(), user.isPlatformAdmin());
        return eventSeriesService.toResponse(series);
    }
}
