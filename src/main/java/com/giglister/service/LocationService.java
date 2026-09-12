package com.giglister.service;

import com.giglister.domain.Event;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.common.EventSummary;
import com.giglister.dto.location.LocationCreateRequest;
import com.giglister.dto.location.LocationListItem;
import com.giglister.dto.location.LocationResponse;
import com.giglister.dto.location.LocationUpdateRequest;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.EventRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final PermissionService permissionService;
    private final SummaryMapper summaryMapper;
    private final GeoService geoService;
    private final GeocodingService geocodingService;

    /** Only called when the caller didn't already give coordinates - a Location that
     * already has them (or a caller that never will, like an update round-tripping the
     * same unset fields) never gets silently re-geocoded over its own values. The full
     * address geocodes far more reliably than a venue name ever would, so it's preferred
     * whenever present. */
    private Optional<GeocodingService.Coordinates> geocodeAddress(String name, String city, String address, String postalCode) {
        String query = (address != null && !address.isBlank())
                ? address + ", " + (postalCode != null && !postalCode.isBlank() ? postalCode + " " : "") + city
                : name + ", " + city;
        return geocodingService.geocode(query);
    }

    public Location getOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location " + id + " not found"));
    }

    /**
     * A real coordinate radius (from "Standort verwenden", §13) takes precedence
     * over the plain city-name match once given - locations without coordinates
     * still show up (see GeoService.withinRadius), so STUBs are never hidden.
     */
    public Page<Location> listPublished(String city, Double centerLat, Double centerLon, Integer radiusKm, Pageable pageable) {
        if (radiusKm == null || centerLat == null || centerLon == null) {
            if (city != null && !city.isBlank()) {
                return locationRepository.findByStatusAndCityIgnoreCase(EntityStatus.PUBLISHED, city, pageable);
            }
            return locationRepository.findByStatus(EntityStatus.PUBLISHED, pageable);
        }
        List<Location> filtered = locationRepository.findByStatus(EntityStatus.PUBLISHED, Pageable.unpaged())
                .getContent().stream()
                .filter(l -> city == null || city.isBlank() || city.equalsIgnoreCase(l.getCity()))
                .filter(l -> geoService.withinRadius(l.getLatitude(), l.getLongitude(), centerLat, centerLon, radiusKm))
                .toList();
        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional
    public Location createStub(String name, String city, String address, String postalCode) {
        GeocodingService.Coordinates coords = geocodeAddress(name, city, address, postalCode).orElse(null);
        Location location = Location.builder()
                .name(name).city(city).address(address).postalCode(postalCode)
                .latitude(coords != null ? coords.latitude() : null)
                .longitude(coords != null ? coords.longitude() : null)
                .status(EntityStatus.STUB).build();
        return locationRepository.save(location);
    }

    @Transactional
    public Location create(LocationCreateRequest request, Long createdBy) {
        Double latitude = request.latitude();
        Double longitude = request.longitude();
        if (latitude == null || longitude == null) {
            Optional<GeocodingService.Coordinates> coords =
                    geocodeAddress(request.name(), request.city(), request.address(), request.postalCode());
            if (coords.isPresent()) {
                latitude = coords.get().latitude();
                longitude = coords.get().longitude();
            }
        }
        Location location = Location.builder()
                .name(request.name())
                .city(request.city())
                .address(request.address())
                .postalCode(request.postalCode())
                .country(request.country())
                .website(request.website())
                .logoUrl(request.logoUrl())
                .titleImageUrl(request.titleImageUrl())
                .latitude(latitude)
                .longitude(longitude)
                .status(EntityStatus.DRAFT)
                .createdBy(createdBy)
                .build();
        location = locationRepository.save(location);
        permissionService.grant(EntityType.LOCATION, location.getId(), createdBy, PermissionLevel.MANAGE, createdBy);
        return location;
    }

    /** Only used when approving a Submission - the image is fetched and stored after create() already ran. */
    @Transactional
    public Location setTitleImage(Long id, String titleImageUrl) {
        Location location = getOrThrow(id);
        location.setTitleImageUrl(titleImageUrl);
        return locationRepository.save(location);
    }

    @Transactional
    public Location update(Long id, LocationUpdateRequest request, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.LOCATION, id, PermissionLevel.EDIT);
        Location location = getOrThrow(id);
        location.setName(request.name());
        location.setCity(request.city());
        location.setAddress(request.address());
        location.setPostalCode(request.postalCode());
        location.setCountry(request.country());
        location.setWebsite(request.website());
        location.setLogoUrl(request.logoUrl());
        location.setTitleImageUrl(request.titleImageUrl());
        Double latitude = request.latitude();
        Double longitude = request.longitude();
        if (latitude == null || longitude == null) {
            Optional<GeocodingService.Coordinates> coords =
                    geocodeAddress(request.name(), request.city(), request.address(), request.postalCode());
            if (coords.isPresent()) {
                latitude = coords.get().latitude();
                longitude = coords.get().longitude();
            }
        }
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return locationRepository.save(location);
    }

    public record BackfillResult(int attempted, int resolved) {
    }

    /** One-off catch-up for locations created before geocoding existed (or whose address
     * didn't resolve at the time) - create()/update() already geocode going forward, so
     * this is only ever needed as a manual admin action, not something called routinely.
     * Spaces its own calls out (Nominatim's usage policy caps at ~1 request/second) since,
     * unlike a single location save, this can run through many of them in one go. */
    @Transactional
    public BackfillResult backfillMissingCoordinates() {
        List<Location> missing = locationRepository.findByLatitudeIsNull();
        int resolved = 0;
        for (Location location : missing) {
            Optional<GeocodingService.Coordinates> coords = geocodeAddress(
                    location.getName(), location.getCity(), location.getAddress(), location.getPostalCode());
            if (coords.isPresent()) {
                location.setLatitude(coords.get().latitude());
                location.setLongitude(coords.get().longitude());
                locationRepository.save(location);
                resolved++;
            }
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new BackfillResult(missing.size(), resolved);
    }

    @Transactional
    public Location updateStatus(Long id, EntityStatus status, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.LOCATION, id, PermissionLevel.MANAGE);
        Location location = getOrThrow(id);
        location.setStatus(status);
        return locationRepository.save(location);
    }

    public List<EventSummary> upcomingEvents(Long locationId) {
        return eventRepository
                .findByLocationIdAndStatusAndDateGreaterThanEqualOrderByDateAsc(locationId, EventStatus.PUBLISHED, LocalDate.now())
                .stream().map(summaryMapper::eventSummary).toList();
    }

    public Map<Integer, List<EventSummary>> pastEventsByYear(Long locationId) {
        List<Event> past = eventRepository
                .findByLocationIdAndStatusAndDateLessThanOrderByDateDesc(locationId, EventStatus.PUBLISHED, LocalDate.now());
        return past.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDate().getYear(),
                        Collectors.collectingAndThen(Collectors.toList(),
                                list -> list.stream().map(summaryMapper::eventSummary).toList())
                ));
    }

    public long upcomingEventCount(Long locationId) {
        return eventRepository
                .findByLocationIdAndStatusAndDateGreaterThanEqualOrderByDateAsc(locationId, EventStatus.PUBLISHED, LocalDate.now())
                .size();
    }

    public LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(), location.getName(), location.getCity(), location.getAddress(),
                location.getPostalCode(), location.getCountry(), location.getWebsite(), location.getLogoUrl(),
                location.getTitleImageUrl(), location.getLatitude(), location.getLongitude(), location.getStatus(),
                permissionService.isUnclaimed(EntityType.LOCATION, location.getId()),
                upcomingEvents(location.getId()), pastEventsByYear(location.getId())
        );
    }

    public LocationListItem toListItem(Location location) {
        return new LocationListItem(location.getId(), location.getName(), location.getCity(), upcomingEventCount(location.getId()));
    }

    public List<Location> listAllPublishedSortedByUpcoming(String city, int limit) {
        List<Location> locations = city == null || city.isBlank()
                ? locationRepository.findByStatusIn(List.of(EntityStatus.PUBLISHED))
                : locationRepository.findByStatus(EntityStatus.PUBLISHED, Pageable.unpaged()).getContent();
        return locations.stream()
                .sorted(Comparator.comparingLong((Location l) -> upcomingEventCount(l.getId())).reversed())
                .limit(limit)
                .toList();
    }

    public Optional<Location> findExactMatch(String name, String city) {
        return locationRepository.searchByName(name).stream()
                .filter(l -> l.getName().equalsIgnoreCase(name) && l.getCity().equalsIgnoreCase(city))
                .findFirst();
    }
}
