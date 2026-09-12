package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.Event;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.dto.common.BandSummary;
import com.giglister.dto.common.EventSummary;
import com.giglister.dto.common.LocationSummary;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the read-only summary DTOs used across event/band/location responses.
 * A Band or Location only becomes "linkable" (has a real profile page) once it
 * is PUBLISHED - see the "no empty profile pages" rule in the concept.
 */
@Component
@RequiredArgsConstructor
public class SummaryMapper {

    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;

    public BandSummary bandSummary(Band band) {
        return new BandSummary(band.getId(), band.getName(), band.getCity(), band.getStatus(),
                band.getLogoUrl(), band.getTitleImageUrl(), band.getStatus() == EntityStatus.PUBLISHED,
                band.getGenres());
    }

    public BandSummary bandSummary(Long bandId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("Band " + bandId + " not found"));
        return bandSummary(band);
    }

    public LocationSummary locationSummary(Location location) {
        return new LocationSummary(location.getId(), location.getName(), location.getCity(),
                location.getStatus(), location.getTitleImageUrl(), location.getStatus() == EntityStatus.PUBLISHED);
    }

    public LocationSummary locationSummary(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location " + locationId + " not found"));
        return locationSummary(location);
    }

    public EventSummary eventSummary(Event event) {
        List<BandSummary> bands = event.getBandIds().stream().map(this::bandSummary).toList();
        return new EventSummary(event.getId(), event.getTitle(), event.getDate(), event.getStartTime(),
                locationSummary(event.getLocationId()), bands, event.getTitleImageUrl(),
                event.getBandImageDisplay(), event.getStatus());
    }
}
