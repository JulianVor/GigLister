package com.giglister.web;

import com.giglister.domain.Event;
import com.giglister.dto.CalendarDayCount;
import com.giglister.dto.GenreFilterOption;
import com.giglister.dto.event.EventCreateRequest;
import com.giglister.dto.event.EventResponse;
import com.giglister.dto.event.EventStatusUpdateRequest;
import com.giglister.dto.event.EventUpdateRequest;
import com.giglister.security.CurrentUser;
import com.giglister.service.EventService;
import com.giglister.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    @GetMapping
    public Page<EventResponse> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Integer radiusKm,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return eventService.listUpcoming(city, lat, lon, radiusKm, from, to, splitGenres(genre), PageRequest.of(page, size))
                .map(eventService::toResponse);
    }

    /** `genre` is comma-separated (e.g. "Punk,Stoner") when more than one is selected - the
     * frontend's GenreFilter lets several be picked at once, which broadens the search
     * (OR, see EventService.filterByGenres) rather than narrowing it to only concerts
     * matching every one of them. */
    private List<String> splitGenres(String genre) {
        if (genre == null || genre.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(genre.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .toList();
    }

    @GetMapping("/genres")
    public List<GenreFilterOption> genres(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Integer radiusKm,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return eventService.availableGenreFilters(city, lat, lon, radiusKm, from, to);
    }

    @GetMapping("/calendar")
    public List<CalendarDayCount> calendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Integer radiusKm
    ) {
        YearMonth ym = YearMonth.of(year, month);
        return eventService.calendarCounts(city, lat, lon, radiusKm, ym.atDay(1), ym.atEndOfMonth());
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.toResponse(eventService.getOrThrow(id));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventCreateRequest request) {
        Event event = eventService.create(request, CurrentUser.requireId());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.toResponse(event));
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest request) {
        var user = CurrentUser.require();
        Event event = eventService.update(id, request, user.getId(), user.isPlatformAdmin());
        return eventService.toResponse(event);
    }

    @PatchMapping("/{id}/status")
    public EventResponse updateStatus(@PathVariable Long id, @Valid @RequestBody EventStatusUpdateRequest request) {
        var user = CurrentUser.require();
        Event event = eventService.updateStatus(id, request.status(), user.getId(), user.isPlatformAdmin());
        return eventService.toResponse(event);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        var user = CurrentUser.require();
        eventService.delete(id, user.getId(), user.isPlatformAdmin());
    }

    @PostMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable Long id) {
        userService.saveEvent(CurrentUser.requireId(), id);
    }

    @DeleteMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable Long id) {
        userService.unsaveEvent(CurrentUser.requireId(), id);
    }
}
