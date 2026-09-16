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
import com.giglister.exception.ConflictException;
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
        return create(request, createdBy, EntityStatus.DRAFT);
    }

    /** Used when approving a GPT-skill submission (see SubmissionService.approveBand) -
     * unlike a real person filling out the create form themselves, an automated proposal
     * has no one vouching that what's there is actually right, so it never starts as a
     * "someone's deliberately working on this" Entwurf. Complete enough to auto-publish
     * (same isComplete bar as applyEnrichment), or STUB otherwise - exactly where a
     * same-quality submission enriching an already-existing stub would land it too. */
    @Transactional
    public Band createFromAutomatedProposal(BandCreateRequest request, Long createdBy) {
        Band band = create(request, createdBy, EntityStatus.STUB);
        if (isComplete(band)) {
            band.setStatus(EntityStatus.PUBLISHED);
            band = bandRepository.save(band);
        }
        return band;
    }

    private Band create(BandCreateRequest request, Long createdBy, EntityStatus status) {
        Band band = Band.builder()
                .name(request.name())
                .city(request.city())
                .country(request.country())
                .shortDescription(request.shortDescription())
                .website(request.website())
                .logoUrl(request.logoUrl())
                .titleImageUrl(request.titleImageUrl())
                .genres(request.genres() != null ? new ArrayList<>(request.genres()) : new ArrayList<>())
                .status(status)
                .createdBy(createdBy)
                .build();
        band = bandRepository.save(band);
        permissionService.grant(EntityType.BAND, band.getId(), createdBy, PermissionLevel.MANAGE, createdBy);
        return band;
    }

    /** Only used when approving a Submission - the image is fetched and stored after create() already ran. */
    @Transactional
    public Band setTitleImage(Long id, String titleImageUrl) {
        Band band = getOrThrow(id);
        band.setTitleImageUrl(titleImageUrl);
        return bandRepository.save(band);
    }

    @Transactional
    public Band update(Long id, BandUpdateRequest request, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, id, PermissionLevel.EDIT);
        Band band = getOrThrow(id);
        band.setName(request.name());
        band.setCity(request.city());
        band.setCountry(request.country());
        band.setShortDescription(request.shortDescription());
        band.setWebsite(request.website());
        band.setLogoUrl(request.logoUrl());
        band.setTitleImageUrl(request.titleImageUrl());
        band.setGenres(request.genres() != null ? new ArrayList<>(request.genres()) : band.getGenres());
        return bandRepository.save(band);
    }

    /** Applies a GPT-skill submission's enrichment payload to an existing STUB/DRAFT band -
     * only the fields it actually filled in are patched, everything else (including
     * `name`, deliberately never touched here - it's just how the target was identified)
     * keeps its current value, unlike update()'s full replace. Bumps the band to PUBLISHED
     * the moment the result looks complete (see isComplete), same as a human admin
     * approving a submission today publishes it - never downgrades an already-PUBLISHED
     * or ARCHIVED band. */
    @Transactional
    public Band applyEnrichment(Long id, BandCreateRequest request, String imageUrl) {
        Band band = getOrThrow(id);
        if (request.city() != null) {
            band.setCity(request.city());
        }
        if (request.country() != null) {
            band.setCountry(request.country());
        }
        if (request.shortDescription() != null) {
            band.setShortDescription(request.shortDescription());
        }
        if (request.website() != null) {
            band.setWebsite(request.website());
        }
        if (request.logoUrl() != null) {
            band.setLogoUrl(request.logoUrl());
        }
        if (imageUrl != null) {
            band.setTitleImageUrl(imageUrl);
        } else if (request.titleImageUrl() != null) {
            band.setTitleImageUrl(request.titleImageUrl());
        }
        if (request.genres() != null && !request.genres().isEmpty()) {
            band.setGenres(new ArrayList<>(request.genres()));
        }
        if ((band.getStatus() == EntityStatus.STUB || band.getStatus() == EntityStatus.DRAFT) && isComplete(band)) {
            band.setStatus(EntityStatus.PUBLISHED);
        }
        return bandRepository.save(band);
    }

    /** What "vollständig" means for a band enrichment to auto-publish it - deliberately not
     * the images, which a GPT skill can rarely source reliably. */
    private boolean isComplete(Band band) {
        return notBlank(band.getCity()) && notBlank(band.getShortDescription()) && !band.getGenres().isEmpty();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @Transactional
    public Band updateStatus(Long id, EntityStatus status, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, id, PermissionLevel.MANAGE);
        Band band = getOrThrow(id);
        band.setStatus(status);
        return bandRepository.save(band);
    }

    /** Same MANAGE tier as updateStatus - deleting is at least as destructive as archiving.
     * Blocked while any event (of any status) still lists this band, same as merge()
     * refuses to leave a concert without a location/band - "kein Konzert darf seine Band
     * verlieren" applies here too, just via a hard stop instead of a relink. Follows and
     * permission grants are cleaned up rather than left as orphaned rows a future band
     * created with a reused id could otherwise inherit. */
    @Transactional
    public void delete(Long id, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, id, PermissionLevel.MANAGE);
        Band band = getOrThrow(id);
        if (!eventRepository.findByBandId(id).isEmpty()) {
            throw new ConflictException("Diese Band hat noch Konzerte und kann daher nicht gelöscht werden.");
        }
        bandFollowRepository.deleteByBandId(id);
        permissionService.revokeAll(EntityType.BAND, id);
        bandRepository.delete(band);
    }

    public List<com.giglister.dto.common.EventSummary> upcomingEvents(Long bandId) {
        return eventRepository.findUpcomingForBand(bandId, EventStatus.PUBLISHED, LocalDate.now()).stream()
                .map(summaryMapper::eventSummary)
                .toList();
    }

    public BandResponse toResponse(Band band) {
        return new BandResponse(
                band.getId(), band.getName(), band.getCity(), band.getCountry(),
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
