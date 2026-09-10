package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.BandFollow;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.band.BandCreateRequest;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.band.BandUpdateRequest;
import com.giglister.exception.ForbiddenException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandFollowRepository;
import com.giglister.repository.BandRepository;
import com.giglister.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BandService {

    private final BandRepository bandRepository;
    private final EventRepository eventRepository;
    private final BandFollowRepository bandFollowRepository;
    private final PermissionService permissionService;
    private final SummaryMapper summaryMapper;

    public Band getOrThrow(Long id) {
        return bandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Band " + id + " not found"));
    }

    public Page<Band> listPublished(String city, Pageable pageable) {
        if (city != null && !city.isBlank()) {
            return bandRepository.findByStatusAndCityIgnoreCase(EntityStatus.PUBLISHED, city, pageable);
        }
        return bandRepository.findByStatus(EntityStatus.PUBLISHED, pageable);
    }

    @Transactional
    public Band createStub(String name, String city) {
        Band band = Band.builder().name(name).city(city).status(EntityStatus.STUB).build();
        return bandRepository.save(band);
    }

    @Transactional
    public Band create(BandCreateRequest request, Long createdBy) {
        Band band = Band.builder()
                .name(request.name())
                .city(request.city())
                .region(request.region())
                .country(request.country())
                .shortDescription(request.shortDescription())
                .website(request.website())
                .genres(request.genres() != null ? new ArrayList<>(request.genres()) : new ArrayList<>())
                .status(EntityStatus.DRAFT)
                .createdBy(createdBy)
                .build();
        band = bandRepository.save(band);
        permissionService.grant(EntityType.BAND, band.getId(), createdBy, PermissionLevel.MANAGE, createdBy);
        return band;
    }

    @Transactional
    public Band update(Long id, BandUpdateRequest request, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, id, PermissionLevel.EDIT);
        Band band = getOrThrow(id);
        band.setName(request.name());
        band.setCity(request.city());
        band.setRegion(request.region());
        band.setCountry(request.country());
        band.setShortDescription(request.shortDescription());
        band.setWebsite(request.website());
        band.setLogoUrl(request.logoUrl());
        band.setTitleImageUrl(request.titleImageUrl());
        band.setGenres(request.genres() != null ? new ArrayList<>(request.genres()) : band.getGenres());
        return bandRepository.save(band);
    }

    @Transactional
    public Band updateStatus(Long id, EntityStatus status, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, id, PermissionLevel.MANAGE);
        Band band = getOrThrow(id);
        band.setStatus(status);
        return bandRepository.save(band);
    }

    public List<com.giglister.dto.common.EventSummary> upcomingEvents(Long bandId) {
        return eventRepository.findUpcomingForBand(bandId, EventStatus.PUBLISHED, LocalDate.now()).stream()
                .map(summaryMapper::eventSummary)
                .toList();
    }

    public BandResponse toResponse(Band band) {
        return new BandResponse(
                band.getId(), band.getName(), band.getCity(), band.getRegion(), band.getCountry(),
                band.getShortDescription(), band.getWebsite(), band.getLogoUrl(), band.getTitleImageUrl(),
                band.getGenres(), band.getStatus(), permissionService.isUnclaimed(EntityType.BAND, band.getId()),
                upcomingEvents(band.getId())
        );
    }

    @Transactional
    public void follow(Long bandId, Long userId) {
        getOrThrow(bandId);
        if (bandFollowRepository.existsByUserIdAndBandId(userId, bandId)) {
            return;
        }
        bandFollowRepository.save(BandFollow.builder().userId(userId).bandId(bandId).build());
    }

    @Transactional
    public void unfollow(Long bandId, Long userId) {
        bandFollowRepository.deleteByUserIdAndBandId(userId, bandId);
    }

    public List<Band> findByIds(List<Long> ids) {
        return bandRepository.findAllById(ids);
    }

    public Optional<Band> findExactMatch(String name, String city) {
        return bandRepository.searchByName(name).stream()
                .filter(b -> b.getName().equalsIgnoreCase(name)
                        && ((city == null && b.getCity() == null) || (city != null && city.equalsIgnoreCase(b.getCity()))))
                .findFirst();
    }
}
