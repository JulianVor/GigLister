package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.BandStory;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.band.BandStoryCreateRequest;
import com.giglister.dto.band.BandStoryResponse;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** A band's ephemeral photo+text status (see Instagram Stories) - auto-expires 24h after
 * posting. No cleanup job removes expired rows; listActive simply never returns them
 * (expiresAt > now), same "filter at query time" approach as everywhere else in this
 * codebase that has a live/expired split. */
@Service
@RequiredArgsConstructor
public class BandStoryService {

    private final BandStoryRepository bandStoryRepository;
    private final BandService bandService;
    private final PermissionService permissionService;

    @Transactional
    public BandStory create(Long bandId, BandStoryCreateRequest request, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, bandId, PermissionLevel.EDIT);
        bandService.getOrThrow(bandId);
        BandStory story = BandStory.builder()
                .bandId(bandId)
                .imageUrl(request.imageUrl())
                .text(request.text())
                .build();
        return bandStoryRepository.save(story);
    }

    public List<BandStory> listActive(Long bandId) {
        Band band = bandService.getOrThrow(bandId);
        bandService.assertVisible(band);
        return bandStoryRepository.findByBandIdAndExpiresAtAfterOrderByCreatedAtAsc(bandId, Instant.now());
    }

    @Transactional
    public void delete(Long bandId, Long storyId, Long userId, boolean platformAdmin) {
        permissionService.require(userId, platformAdmin, EntityType.BAND, bandId, PermissionLevel.EDIT);
        BandStory story = bandStoryRepository.findById(storyId)
                .filter(s -> s.getBandId().equals(bandId))
                .orElseThrow(() -> new NotFoundException("Story " + storyId + " not found"));
        bandStoryRepository.delete(story);
    }

    public BandStoryResponse toResponse(BandStory story) {
        return new BandStoryResponse(story.getId(), story.getImageUrl(), story.getText(), story.getCreatedAt(), story.getExpiresAt());
    }
}
