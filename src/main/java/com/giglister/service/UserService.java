package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.EntityPermission;
import com.giglister.domain.Event;
import com.giglister.domain.SavedEvent;
import com.giglister.domain.User;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.dto.MeResponse;
import com.giglister.dto.ProfileUpdateRequest;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandFollowRepository;
import com.giglister.repository.EntityPermissionRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.SavedEventRepository;
import com.giglister.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SavedEventRepository savedEventRepository;
    private final BandFollowRepository bandFollowRepository;
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

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getOrThrow(userId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName());
        }
        user.setHomeCity(request.homeCity());
        user.setHomeLatitude(request.homeLatitude());
        user.setHomeLongitude(request.homeLongitude());
        user.setRadiusKm(request.radiusKm());
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

        List<MeResponse.ManagedFollowedBand> followedBands = bandFollowRepository.findByUserId(user.getId()).stream()
                .map(f -> {
                    Band band = bandService.getOrThrow(f.getBandId());
                    List<Event> upcoming = eventRepository.findUpcomingForBand(band.getId(), EventStatus.PUBLISHED, LocalDate.now());
                    String next = upcoming.isEmpty() ? null : upcoming.get(0).getDate().toString();
                    return new MeResponse.ManagedFollowedBand(band.getId(), band.getName(), next);
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

        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getHomeCity(),
                user.getRadiusKm(), user.isPlatformAdmin(), saved, followedBands, managed);
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
