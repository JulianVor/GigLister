package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.Event;
import com.giglister.domain.Location;
import com.giglister.domain.enums.BandImageDisplay;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.CalendarDayCount;
import com.giglister.dto.common.EntityRef;
import com.giglister.dto.event.EventCreateRequest;
import com.giglister.dto.event.EventResponse;
import com.giglister.dto.event.EventUpdateRequest;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.ForbiddenException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final LocationService locationService;
    private final BandService bandService;
    private final PermissionService permissionService;
    private final GeoService geoService;
    private final SummaryMapper summaryMapper;

    public Event getOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event " + id + " not found"));
    }

    /** Resolves an EntityRef to an existing Location id, creating a STUB when only a name is given. */
    private Long resolveLocation(EntityRef ref) {
        if (ref.isExisting()) {
            locationService.getOrThrow(ref.id());
            return ref.id();
        }
        if (ref.name() == null || ref.name().isBlank() || ref.city() == null || ref.city().isBlank()) {
            throw new BadRequestException("New locations require at least a name and a city");
        }
        if (ref.address() == null || ref.address().isBlank() || ref.postalCode() == null || ref.postalCode().isBlank()) {
            throw new BadRequestException("New locations require a street address and a postal code");
        }
        return locationService.findExactMatch(ref.name(), ref.city())
                .map(Location::getId)
                .orElseGet(() -> locationService.createStub(ref.name(), ref.city(), ref.address(), ref.postalCode()).getId());
    }

    private Long resolveBand(EntityRef ref) {
        if (ref.isExisting()) {
            bandService.getOrThrow(ref.id());
            return ref.id();
        }
        if (ref.name() == null || ref.name().isBlank()) {
            throw new BadRequestException("New bands require at least a name");
        }
        return bandService.findExactMatch(ref.name(), ref.city())
                .map(Band::getId)
                .orElseGet(() -> bandService.createStub(ref.name(), ref.city()).getId());
    }

    @Transactional
    public Event create(EventCreateRequest request, Long createdBy) {
        Long locationId = resolveLocation(request.location());
        List<Long> bandIds = new ArrayList<>();
        for (EntityRef ref : request.bands()) {
            bandIds.add(resolveBand(ref));
        }
        Event event = Event.builder()
                .title(blankToNull(request.title()))
                .date(request.date())
                .startTime(request.startTime())
                .locationId(locationId)
                .bandIds(bandIds)
                .description(request.description())
                .ticketUrl(request.ticketUrl())
                .titleImageUrl(request.titleImageUrl())
                .bandImageDisplay(request.bandImageDisplay() != null ? request.bandImageDisplay() : BandImageDisplay.PHOTO)
                .status(EventStatus.PUBLISHED)
                .createdBy(createdBy)
                .build();
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, EventUpdateRequest request, Long userId, boolean platformAdmin) {
        Event event = getOrThrow(id);
        requireEditRights(event, userId, platformAdmin);

        Long locationId = resolveLocation(request.location());
        List<Long> bandIds = new ArrayList<>();
        for (EntityRef ref : request.bands()) {
            bandIds.add(resolveBand(ref));
        }
        event.setTitle(blankToNull(request.title()));
        event.setDate(request.date());
        event.setStartTime(request.startTime());
        event.setLocationId(locationId);
        event.setBandIds(bandIds);
        event.setDescription(request.description());
        event.setTicketUrl(request.ticketUrl());
        event.setTitleImageUrl(request.titleImageUrl());
        event.setBandImageDisplay(request.bandImageDisplay() != null ? request.bandImageDisplay() : BandImageDisplay.PHOTO);
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateStatus(Long id, EventStatus status, Long userId, boolean platformAdmin) {
        Event event = getOrThrow(id);
        requireEditRights(event, userId, platformAdmin);
        event.setStatus(status);
        return eventRepository.save(event);
    }

    /** Creator, platform admin, or anyone with EDIT+ on the location or any of the line-up's bands. */
    public void requireEditRights(Event event, Long userId, boolean platformAdmin) {
        if (platformAdmin || event.getCreatedBy().equals(userId)) {
            return;
        }
        boolean allowed = permissionService.has(userId, false, EntityType.LOCATION, event.getLocationId(), PermissionLevel.EDIT)
                || event.getBandIds().stream()
                .anyMatch(bandId -> permissionService.has(userId, false, EntityType.BAND, bandId, PermissionLevel.EDIT));
        if (!allowed) {
            throw new ForbiddenException("You may not edit this event");
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public List<Event> filterByLocationRadius(List<Event> events, String city, Double centerLat, Double centerLon, Integer radiusKm) {
        if ((city == null || city.isBlank()) && radiusKm == null) {
            return events;
        }
        return events.stream().filter(e -> {
            Location loc = locationService.getOrThrow(e.getLocationId());
            boolean cityMatches = city == null || city.isBlank() || city.equalsIgnoreCase(loc.getCity());
            boolean withinRadius = radiusKm == null
                    || geoService.withinRadius(loc.getLatitude(), loc.getLongitude(), centerLat, centerLon, radiusKm);
            return cityMatches && withinRadius;
        }).toList();
    }

    public Page<Event> listUpcoming(String city, Double centerLat, Double centerLon, Integer radiusKm,
                                     LocalDate from, LocalDate to, Pageable pageable) {
        LocalDate start = from != null ? from : LocalDate.now();
        List<Event> all;
        if (to != null) {
            all = eventRepository
                    .findByStatusAndDateBetweenOrderByDateAscStartTimeAsc(EventStatus.PUBLISHED, start, to, Pageable.unpaged())
                    .getContent();
        } else {
            all = eventRepository
                    .findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(EventStatus.PUBLISHED, start, Pageable.unpaged())
                    .getContent();
        }
        List<Event> filtered = filterByLocationRadius(all, city, centerLat, centerLon, radiusKm);
        int pageStart = (int) pageable.getOffset();
        if (pageStart >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int pageEnd = Math.min(pageStart + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(pageStart, pageEnd), pageable, filtered.size());
    }

    /** Without a location filter this uses a single GROUP BY count query; with one, distance
     * filtering has to happen in Java (like listUpcoming), so counts are grouped here instead. */
    public List<CalendarDayCount> calendarCounts(String city, Double centerLat, Double centerLon, Integer radiusKm,
                                                  LocalDate from, LocalDate to) {
        if ((city == null || city.isBlank()) && radiusKm == null) {
            return eventRepository.countByDateBetween(EventStatus.PUBLISHED, from, to).stream()
                    .map(dc -> new CalendarDayCount(dc.getDay(), dc.getCnt()))
                    .toList();
        }
        List<Event> all = eventRepository
                .findByStatusAndDateBetweenOrderByDateAscStartTimeAsc(EventStatus.PUBLISHED, from, to, Pageable.unpaged())
                .getContent();
        List<Event> filtered = filterByLocationRadius(all, city, centerLat, centerLon, radiusKm);
        return filtered.stream()
                .collect(Collectors.groupingBy(Event::getDate, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new CalendarDayCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CalendarDayCount::date))
                .toList();
    }

    public EventResponse toResponse(Event event) {
        var bands = event.getBandIds().stream().map(summaryMapper::bandSummary).toList();
        return new EventResponse(
                event.getId(), event.getTitle(), event.getDate(), event.getStartTime(),
                summaryMapper.locationSummary(event.getLocationId()), bands, event.getDescription(),
                event.getTicketUrl(), event.getTitleImageUrl(), event.getBandImageDisplay(),
                event.getStatus(), event.getCreatedBy()
        );
    }

    public Optional<Event> find(Long id) {
        return eventRepository.findById(id);
    }
}
