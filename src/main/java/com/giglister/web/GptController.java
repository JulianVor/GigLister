package com.giglister.web;

import com.giglister.domain.Band;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.location.LocationResponse;
import com.giglister.repository.BandRepository;
import com.giglister.repository.LocationRepository;
import com.giglister.service.BandService;
import com.giglister.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Read side of the GPT-skill enrichment flow (see Submission.targetEntityId): lets it find
 * STUB/DRAFT bands/locations to research and fill in, without needing a real user account -
 * BandController/LocationController's own GET .../{id} hide non-PUBLISHED entities from
 * anonymous callers, and the GPT-skill identity isn't a real AppUserPrincipal (see
 * GptSkillAuthFilter), so it would otherwise never see these at all. Restricted to
 * ROLE_GPT_SKILL in SecurityConfig, same as POST /api/submissions - still read-only, still
 * never changes anything by itself.
 */
@RestController
@RequestMapping("/api/gpt")
@RequiredArgsConstructor
public class GptController {

    /** Keeps one response comfortably inside a GPT Action's context - matches the low
     * hundreds this platform's data actually reaches, not a hard product limit. */
    private static final int LIST_LIMIT = 50;

    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;
    private final BandService bandService;
    private final LocationService locationService;

    @GetMapping("/bands/incomplete")
    public List<BandResponse> incompleteBands() {
        return bandRepository.findByStatusIn(List.of(EntityStatus.STUB, EntityStatus.DRAFT)).stream()
                .sorted(Comparator.comparing(Band::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(LIST_LIMIT)
                .map(bandService::toResponse)
                .toList();
    }

    @GetMapping("/locations/incomplete")
    public List<LocationResponse> incompleteLocations() {
        return locationRepository.findByStatusIn(List.of(EntityStatus.STUB, EntityStatus.DRAFT)).stream()
                .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(LIST_LIMIT)
                .map(locationService::toResponse)
                .toList();
    }
}
