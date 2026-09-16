package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.BandLineupEntry;
import com.giglister.domain.Event;
import com.giglister.domain.EventSeries;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.dto.common.BandSummary;
import com.giglister.dto.common.EventSeriesSummary;
import com.giglister.dto.common.EventSummary;
import com.giglister.dto.common.LocationSummary;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandRepository;
import com.giglister.repository.EventSeriesRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
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
    private final EventSeriesRepository eventSeriesRepository;

    public BandSummary bandSummary(Band band) {
        return bandSummary(band, null);
    }

    private BandSummary bandSummary(Band band, LocalTime startTime) {
        return new BandSummary(band.getId(), band.getName(), band.getCity(), band.getStatus(),
                band.getLogoUrl(), band.getTitleImageUrl(), band.getStatus() == EntityStatus.PUBLISHED,
                band.getGenres(), startTime);
    }

    public BandSummary bandSummary(Long bandId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("Band " + bandId + " not found"));
        return bandSummary(band, null);
    }

    /** Like bandSummary(Long), but also carries this specific event's own per-band start
     * time (see BandLineupEntry) through onto the response. */
    public BandSummary bandSummary(BandLineupEntry entry) {
        Band band = bandRepository.findById(entry.getBandId())
                .orElseThrow(() -> new NotFoundException("Band " + entry.getBandId() + " not found"));
        return bandSummary(band, entry.getStartTime());
    }

    public LocationSummary locationSummary(Location location) {
        return new LocationSummary(location.getId(), location.getName(), location.getCity(),
                location.getStatus(), location.getTitleImageUrl(), location.getStatus() == EntityStatus.PUBLISHED,
                location.getLatitude(), location.getLongitude());
    }

    public LocationSummary locationSummary(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location " + locationId + " not found"));
        return locationSummary(location);
    }

    public EventSummary eventSummary(Event event) {
        List<BandSummary> bands = event.getBandLineup().stream().map(this::bandSummary).toList();
        return new EventSummary(event.getId(), event.getTitle(), event.getDate(), event.getStartTime(),
                locationSummary(event.getLocationId()), bands, event.getTitleImageUrl(),
                event.getBandImageDisplay(), event.getStatus(), eventSeriesSummary(event.getEventSeriesId()));
    }

    /** Best-effort, not a NotFoundException like bandSummary(Long)/locationSummary(Long) -
     * this reference is optional on Event (unlike a band/location, which every event
     * always has), so a dangling id (e.g. the series was deleted) should just mean "not
     * part of a series" rather than breaking every read of that event. */
    public EventSeriesSummary eventSeriesSummary(Long eventSeriesId) {
        if (eventSeriesId == null) {
            return null;
        }
        return eventSeriesRepository.findById(eventSeriesId)
                .map(s -> new EventSeriesSummary(s.getId(), s.getName(), s.getTitleImageUrl(), s.getTicketUrl()))
                .orElse(null);
    }
}
