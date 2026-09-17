package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.BandFollow;
import com.giglister.domain.BandStory;
import com.giglister.domain.EntityPermission;
import com.giglister.domain.Event;
import com.giglister.domain.SavedAct;
import com.giglister.domain.SavedEvent;
import com.giglister.domain.User;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.dto.MeResponse;
import com.giglister.dto.ProfileUpdateRequest;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.ConflictException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandFollowRepository;
import com.giglister.repository.BandStoryRepository;
import com.giglister.repository.EntityPermissionRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.SavedActRepository;
import com.giglister.repository.SavedEventRepository;
import com.giglister.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SavedEventRepository savedEventRepository;
    private final SavedActRepository savedActRepository;
    private final BandFollowRepository bandFollowRepository;
    private final BandStoryRepository bandStoryRepository;
    private final EntityPermissionRepository entityPermissionRepository;
    private final EventRepository eventRepository;
    private final BandService bandService;
    private final LocationService locationService;
    private final SummaryMapper summaryMapper;

    public User getOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    @Transactional
    public void saveEvent(Long userId, Long eventId) {
        eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (savedEventRepository.existsByUserIdAndEventId(userId, eventId)) {
            return;
        }
        savedEventRepository.save(SavedEvent.builder().userId(userId).eventId(eventId).build());
    }

    @Transactional
    public void unsaveEvent(Long userId, Long eventId) {
        savedEventRepository.deleteByUserIdAndEventId(userId, eventId);
    }

    /** Merken for a single band within a festival concert - only meaningful when the band
     * actually has its own start time in that concert's line-up (otherwise there's nothing
     * to single out from the rest of the bill). Also saves the whole Event, so a concert
     * you've picked even one act out of still shows up everywhere a saved event does. */
    @Transactional
    public void saveAct(Long userId, Long eventId, Long bandId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (event.getEventSeriesId() == null) {
            throw new BadRequestException("Only concerts that are part of a festival have individual acts to merken");
        }
        boolean bandHasOwnStartTime = event.getBandLineup().stream()
                .anyMatch(entry -> entry.getBandId().equals(bandId) && entry.getStartTime() != null);
        if (!bandHasOwnStartTime) {
            throw new BadRequestException("This band has no own start time in this concert");
        }
        if (!savedActRepository.existsByUserIdAndEventIdAndBandId(userId, eventId, bandId)) {
            savedActRepository.save(SavedAct.builder().userId(userId).eventId(eventId).bandId(bandId).build());
        }
        saveEvent(userId, eventId);
    }

    /** Only removes this one act - the whole concert (and any other act saved within it)
     * stays saved, since saving is one-directional (act -> whole event, never the reverse). */
    @Transactional
    public void unsaveAct(Long userId, Long eventId, Long bandId) {
        savedActRepository.deleteByUserIdAndEventIdAndBandId(userId, eventId, bandId);
    }

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getOrThrow(userId);
        if (request.username() != null && !request.username().isBlank() && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsernameIgnoreCase(request.username())) {
                throw new ConflictException("This username is already taken");
            }
            user.setUsername(request.username());
        }
        // Null-guarded like username above - a caller only sending the field(s) it actually
        // changed (e.g. just radiusKm) must not wipe out the others.
        if (request.homeCity() != null) {
            user.setHomeCity(request.homeCity());
        }
        if (request.homeLatitude() != null) {
            user.setHomeLatitude(request.homeLatitude());
        }
        if (request.homeLongitude() != null) {
            user.setHomeLongitude(request.homeLongitude());
        }
        if (request.radiusKm() != null) {
            user.setRadiusKm(request.radiusKm());
        }
        if (request.preferredGenres() != null) {
            // Only ever store canonical base genres - anything else can't ever match the
            // substring check GenreTaxonomy/recommendation scoring do against it anyway.
            // Must be a mutable list (not Stream.toList()'s immutable one) - Hibernate
            // manages this field as a persistent collection and clears/repopulates it in
            // place on merge, which throws UnsupportedOperationException on an immutable one.
            user.setPreferredGenres(request.preferredGenres().stream()
                    .filter(GenreTaxonomy.BASE_GENRES::contains)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        return userRepository.save(user);
    }

    public MeResponse toMeResponse(User user) {
        List<com.giglister.dto.common.EventSummary> saved = savedEventRepository.findByUserId(user.getId()).stream()
                .map(SavedEvent::getEventId)
                .map(eventRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .map(summaryMapper::eventSummary)
                .toList();

        List<MeResponse.SavedAct> savedActs = savedActRepository.findByUserId(user.getId()).stream()
                .map(a -> new MeResponse.SavedAct(a.getEventId(), a.getBandId()))
                .toList();

        List<BandFollow> follows = bandFollowRepository.findByUserId(user.getId());
        // One query for every followed band's live-story status instead of one per band -
        // see BandStoryRepository.findByBandIdInAndExpiresAtAfter.
        Set<Long> bandsWithActiveStory = bandStoryRepository
                .findByBandIdInAndExpiresAtAfter(follows.stream().map(BandFollow::getBandId).toList(), Instant.now())
                .stream().map(BandStory::getBandId).collect(Collectors.toSet());
        List<MeResponse.ManagedFollowedBand> followedBands = follows.stream()
                .map(f -> {
                    Band band = bandService.getOrThrow(f.getBandId());
                    List<Event> upcoming = eventRepository.findUpcomingForBand(band.getId(), EventStatus.PUBLISHED, LocalDate.now());
                    String next = upcoming.isEmpty() ? null : upcoming.get(0).getDate().toString();
                    return new MeResponse.ManagedFollowedBand(
                            band.getId(), band.getName(), band.getLogoUrl(), band.getProfileImageUrl(),
                            bandsWithActiveStory.contains(band.getId()), next
                    );
                })
                .toList();

        List<EntityPermission> permissions = entityPermissionRepository.findByUserId(user.getId());
        List<MeResponse.ManagedEntity> managed = permissions.stream()
                .map(p -> {
                    String name = p.getEntityType() == EntityType.BAND
                            ? safeName(() -> bandService.getOrThrow(p.getEntityId()).getName())
                            : safeName(() -> locationService.getOrThrow(p.getEntityId()).getName());
                    return new MeResponse.ManagedEntity(p.getEntityType(), p.getEntityId(), name, p.getPermission());
                })
                .toList();

        return new MeResponse(user.getId(), user.getEmail(), user.getUsername(), user.getHomeCity(),
                user.getHomeLatitude(), user.getHomeLongitude(),
                user.getRadiusKm(), user.getPreferredGenres(), user.isPlatformAdmin(), user.isMustChangePassword(),
                saved, savedActs, followedBands, managed);
    }

    private List<Long> myManagedBandIds(Long userId) {
        return entityPermissionRepository.findByUserId(userId).stream()
                .filter(p -> p.getEntityType() == EntityType.BAND)
                .map(EntityPermission::getEntityId)
                .distinct()
                .toList();
    }

    /** "Meine Bands": every band the user holds EDIT/MANAGE on, full detail. */
    public List<BandResponse> myManagedBands(Long userId) {
        return bandService.findByIds(myManagedBandIds(userId)).stream()
                .sorted(Comparator.comparing(Band::getName, String.CASE_INSENSITIVE_ORDER))
                .map(bandService::toResponse)
                .toList();
    }

    /** "Meine Veranstaltungen": upcoming events across all of the user's bands, band-übergreifend. */
    public List<EventSummary> myBandEvents(Long userId) {
        List<Long> bandIds = myManagedBandIds(userId);
        if (bandIds.isEmpty()) {
            return List.of();
        }
        return eventRepository.findUpcomingForAnyBand(bandIds, EventStatus.PUBLISHED, LocalDate.now()).stream()
                .map(summaryMapper::eventSummary)
                .toList();
    }

    private String safeName(java.util.function.Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (NotFoundException e) {
            return "(deleted)";
        }
    }
}
