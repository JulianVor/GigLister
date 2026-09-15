package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.BandFollow;
import com.giglister.domain.Event;
import com.giglister.domain.SavedEvent;
import com.giglister.domain.User;
import com.giglister.domain.enums.EventStatus;
import com.giglister.dto.DiscoverResponse;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.repository.BandFollowRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.SavedEventRepository;
import com.giglister.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Entdecken": mostly editorial-feeling sections assembled purely from what is already in
 * the data - plus one rule-based (not ML) "Das könnte dich interessieren" section, see
 * recommendedForYou.
 */
@Service
@RequiredArgsConstructor
public class DiscoverService {

    private static final int SECTION_LIMIT = 10;
    /** How many upcoming events are even considered as recommendation candidates - bounds
     * the scoring work, same cap SubmissionService/EventService use elsewhere for "all
     * upcoming" scans. */
    private static final int CANDIDATE_LIMIT = 200;
    private static final int FOLLOWED_BAND_SCORE = 100;
    private static final int GENRE_MATCH_SCORE = 40;
    private static final int MAX_SCORED_GENRE_MATCHES = 3;

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final LocationService locationService;
    private final BandService bandService;
    private final SummaryMapper summaryMapper;
    private final UserRepository userRepository;
    private final BandFollowRepository bandFollowRepository;
    private final SavedEventRepository savedEventRepository;

    public DiscoverResponse discover(String city, Double lat, Double lon, Integer radiusKm, Long userId) {
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
                .findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(EventStatus.PUBLISHED, today, PageRequest.of(0, CANDIDATE_LIMIT))
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

        List<EventSummary> recommendedForYou = recommendedForYou(userId, city, lat, lon, radiusKm, upcoming);

        return new DiscoverResponse(todayNearby, thisWeekend, newEvents, locationsWithUpcoming, bandsPlayingSoon, recommendedForYou);
    }

    /** "Das könnte dich interessieren": rule-based, not a real recommender - scores each
     * candidate on two signals (a followed band is playing; a base genre the user picked or
     * that shows up among their follows/saves matches a band in the line-up), then falls
     * back to "most saved nearby" for a logged-out visitor or one with no signal yet, so the
     * section is never just empty for lack of personalization data. */
    private List<EventSummary> recommendedForYou(Long userId, String city, Double lat, Double lon,
                                                   Integer radiusKm, List<Event> upcoming) {
        List<Event> candidates = eventService.filterByLocationRadius(upcoming, city, lat, lon, radiusKm);

        if (userId == null) {
            return popularNearby(candidates);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return popularNearby(candidates);
        }

        Set<Long> followedBandIds = bandFollowRepository.findByUserId(userId).stream()
                .map(BandFollow::getBandId).collect(Collectors.toSet());
        Set<Long> savedEventIds = savedEventRepository.findByUserId(userId).stream()
                .map(SavedEvent::getEventId).collect(Collectors.toSet());

        // Every band that could contribute a genre signal: explicitly followed, or playing
        // an event the user already saved (an implicit "I like this" the same as a follow).
        Set<Long> signalBandIds = new LinkedHashSet<>(followedBandIds);
        for (Long eventId : savedEventIds) {
            eventRepository.findById(eventId).ifPresent(e -> signalBandIds.addAll(e.getBandIds()));
        }

        Set<Long> allRelevantBandIds = new LinkedHashSet<>(signalBandIds);
        for (Event e : candidates) {
            allRelevantBandIds.addAll(e.getBandIds());
        }
        Map<Long, List<String>> genresByBandId = bandService.findByIds(List.copyOf(allRelevantBandIds)).stream()
                .collect(Collectors.toMap(Band::getId, Band::getGenres));

        Set<String> genreSignals = new LinkedHashSet<>(user.getPreferredGenres());
        for (Long bandId : signalBandIds) {
            List<String> genres = genresByBandId.getOrDefault(bandId, List.of());
            for (String base : GenreTaxonomy.BASE_GENRES) {
                if (GenreTaxonomy.matches(genres, base)) {
                    genreSignals.add(base);
                }
            }
        }

        if (followedBandIds.isEmpty() && genreSignals.isEmpty()) {
            return popularNearby(candidates);
        }

        Map<Event, Integer> scores = new HashMap<>();
        for (Event e : candidates) {
            if (savedEventIds.contains(e.getId())) {
                continue; // already saved - nothing left to "discover" about it
            }
            int score = 0;
            if (e.getBandIds().stream().anyMatch(followedBandIds::contains)) {
                score += FOLLOWED_BAND_SCORE;
            }
            long genreMatches = genreSignals.stream()
                    .filter(g -> e.getBandIds().stream()
                            .anyMatch(id -> GenreTaxonomy.matches(genresByBandId.getOrDefault(id, List.of()), g)))
                    .count();
            score += (int) Math.min(genreMatches, MAX_SCORED_GENRE_MATCHES) * GENRE_MATCH_SCORE;
            if (score > 0) {
                scores.put(e, score);
            }
        }

        List<EventSummary> scored = scores.entrySet().stream()
                .sorted(Map.Entry.<Event, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getDate()))
                .limit(SECTION_LIMIT)
                .map(entry -> summaryMapper.eventSummary(entry.getKey()))
                .toList();

        return scored.isEmpty() ? popularNearby(candidates) : scored;
    }

    /** Fallback when there's no personalization signal (logged out, or a fresh account with
     * no follows/saves/preferred genres yet): the events the most other users have merkt,
     * so the section still shows something worth clicking instead of just being empty. */
    private List<EventSummary> popularNearby(List<Event> candidates) {
        List<Long> eventIds = candidates.stream().map(Event::getId).toList();
        if (eventIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> saveCounts = savedEventRepository.countByEventIds(eventIds).stream()
                .collect(Collectors.toMap(SavedEventRepository.EventSaveCount::getEventId, SavedEventRepository.EventSaveCount::getCnt));
        return candidates.stream()
                .filter(e -> saveCounts.getOrDefault(e.getId(), 0L) > 0)
                .sorted(Comparator.<Event>comparingLong(e -> saveCounts.getOrDefault(e.getId(), 0L)).reversed()
                        .thenComparing(Event::getDate))
                .limit(SECTION_LIMIT)
                .map(summaryMapper::eventSummary)
                .toList();
    }
}
