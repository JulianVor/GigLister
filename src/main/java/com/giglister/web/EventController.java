package com.giglister.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giglister.domain.Event;
import com.giglister.domain.Submission;
import com.giglister.domain.enums.SubmissionType;
import com.giglister.dto.CalendarDayCount;
import com.giglister.dto.GenreFilterOption;
import com.giglister.dto.event.EventCreateRequest;
import com.giglister.dto.event.EventCreateResult;
import com.giglister.dto.event.EventResponse;
import com.giglister.dto.event.EventStatusUpdateRequest;
import com.giglister.dto.event.EventUpdateRequest;
import com.giglister.dto.submission.SubmissionCreateRequest;
import com.giglister.security.AppUserPrincipal;
import com.giglister.security.CurrentUser;
import com.giglister.service.EventService;
import com.giglister.service.SubmissionService;
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
    private final SubmissionService submissionService;
    private final ObjectMapper objectMapper;

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

    /**
     * Publishes immediately for anyone with direct create rights (platform admin, or EDIT+ on
     * the location or a referenced band - see EventService.canCreateDirectly); otherwise the
     * request is routed into the review queue as a PENDING Submission instead, exactly like a
     * GPT-skill proposal, and only goes live once a platform admin approves it.
     */
    @PostMapping
    public ResponseEntity<EventCreateResult> create(@Valid @RequestBody EventCreateRequest request) {
        AppUserPrincipal user = CurrentUser.require();
        if (eventService.canCreateDirectly(request, user.getId(), user.isPlatformAdmin())) {
            Event event = eventService.create(request, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new EventCreateResult(true, eventService.toResponse(event), null));
        }
        SubmissionCreateRequest submissionRequest = new SubmissionCreateRequest(
                SubmissionType.EVENT, objectMapper.valueToTree(request), null, null);
        Submission submission = submissionService.submit(submissionRequest, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EventCreateResult(false, null, submissionService.toResponse(submission)));
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

    @PostMapping("/{id}/bands/{bandId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveAct(@PathVariable Long id, @PathVariable Long bandId) {
        userService.saveAct(CurrentUser.requireId(), id, bandId);
    }

    @DeleteMapping("/{id}/bands/{bandId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsaveAct(@PathVariable Long id, @PathVariable Long bandId) {
        userService.unsaveAct(CurrentUser.requireId(), id, bandId);
    }
}
