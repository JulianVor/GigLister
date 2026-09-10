package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.BandFollow;
import com.giglister.domain.EntityMerge;
import com.giglister.domain.Event;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandFollowRepository;
import com.giglister.repository.BandRepository;
import com.giglister.repository.EntityMergeRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Admin-only duplicate merge. The source entity survives (archived, with its old
 * name kept as an alias for redirects) while every relationship - events,
 * permissions, follows - is relinked to the target, per "kein Konzert darf seine
 * Location verlieren".
 */
@Service
@RequiredArgsConstructor
public class MergeService {

    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final BandFollowRepository bandFollowRepository;
    private final EntityMergeRepository entityMergeRepository;
    private final PermissionService permissionService;

    @Transactional
    public EntityMerge merge(EntityType type, Long sourceId, Long targetId, Long mergedBy) {
        if (sourceId.equals(targetId)) {
            throw new BadRequestException("Cannot merge an entity into itself");
        }
        String sourceName = type == EntityType.BAND ? mergeBands(sourceId, targetId) : mergeLocations(sourceId, targetId);

        permissionService.relinkOnMerge(type, sourceId, targetId);

        EntityMerge merge = EntityMerge.builder()
                .entityType(type)
                .sourceEntityId(sourceId)
                .targetEntityId(targetId)
                .sourceNameAlias(sourceName)
                .mergedBy(mergedBy)
                .build();
        return entityMergeRepository.save(merge);
    }

    private String mergeBands(Long sourceId, Long targetId) {
        Band source = bandRepository.findById(sourceId).orElseThrow(() -> new NotFoundException("Band " + sourceId + " not found"));
        bandRepository.findById(targetId).orElseThrow(() -> new NotFoundException("Band " + targetId + " not found"));

        List<Event> events = eventRepository.findByBandId(sourceId);
        for (Event event : events) {
            LinkedHashSet<Long> ids = new LinkedHashSet<>(event.getBandIds());
            ids.remove(sourceId);
            ids.add(targetId);
            event.setBandIds(new ArrayList<>(ids));
            eventRepository.save(event);
        }

        for (BandFollow follow : bandFollowRepository.findAll()) {
            if (follow.getBandId().equals(sourceId)) {
                if (bandFollowRepository.existsByUserIdAndBandId(follow.getUserId(), targetId)) {
                    bandFollowRepository.delete(follow);
                } else {
                    follow.setBandId(targetId);
                    bandFollowRepository.save(follow);
                }
            }
        }

        source.setStatus(EntityStatus.ARCHIVED);
        bandRepository.save(source);
        return source.getName();
    }

    private String mergeLocations(Long sourceId, Long targetId) {
        Location source = locationRepository.findById(sourceId).orElseThrow(() -> new NotFoundException("Location " + sourceId + " not found"));
        locationRepository.findById(targetId).orElseThrow(() -> new NotFoundException("Location " + targetId + " not found"));

        List<Event> events = eventRepository.findByLocationId(sourceId);
        for (Event event : events) {
            event.setLocationId(targetId);
            eventRepository.save(event);
        }

        source.setStatus(EntityStatus.ARCHIVED);
        locationRepository.save(source);
        return source.getName();
    }
}
