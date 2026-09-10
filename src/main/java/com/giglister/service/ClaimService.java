package com.giglister.service;

import com.giglister.domain.Claim;
import com.giglister.domain.User;
import com.giglister.domain.enums.ClaimStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.admin.ClaimResponse;
import com.giglister.exception.ConflictException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.ClaimRepository;
import com.giglister.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "Band beanspruchen" / "Location beanspruchen": a user requests MANAGE rights
 * on an entity nobody currently manages. Reviewed manually by a platform admin.
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final BandService bandService;
    private final LocationService locationService;

    @Transactional
    public Claim request(EntityType type, Long entityId, Long userId, String message) {
        assertExists(type, entityId);
        claimRepository.findByEntityTypeAndEntityIdAndRequestedByAndStatus(type, entityId, userId, ClaimStatus.PENDING)
                .ifPresent(c -> {
                    throw new ConflictException("A claim for this entity is already pending");
                });
        Claim claim = Claim.builder()
                .entityType(type)
                .entityId(entityId)
                .requestedBy(userId)
                .message(message)
                .status(ClaimStatus.PENDING)
                .build();
        return claimRepository.save(claim);
    }

    public List<Claim> pending() {
        return claimRepository.findByStatus(ClaimStatus.PENDING);
    }

    @Transactional
    public Claim approve(Long claimId, Long decidedBy) {
        Claim claim = getOrThrow(claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ConflictException("Claim already decided");
        }
        permissionService.grant(claim.getEntityType(), claim.getEntityId(), claim.getRequestedBy(),
                PermissionLevel.MANAGE, decidedBy);
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setDecidedBy(decidedBy);
        claim.setDecidedAt(java.time.Instant.now());
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim reject(Long claimId, Long decidedBy) {
        Claim claim = getOrThrow(claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ConflictException("Claim already decided");
        }
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setDecidedBy(decidedBy);
        claim.setDecidedAt(java.time.Instant.now());
        return claimRepository.save(claim);
    }

    public Claim getOrThrow(Long id) {
        return claimRepository.findById(id).orElseThrow(() -> new NotFoundException("Claim " + id + " not found"));
    }

    private void assertExists(EntityType type, Long entityId) {
        if (type == EntityType.BAND) {
            bandService.getOrThrow(entityId);
        } else {
            locationService.getOrThrow(entityId);
        }
    }

    public ClaimResponse toResponse(Claim claim) {
        String name = switch (claim.getEntityType()) {
            case BAND -> bandService.getOrThrow(claim.getEntityId()).getName();
            case LOCATION -> locationService.getOrThrow(claim.getEntityId()).getName();
        };
        User requester = userRepository.findById(claim.getRequestedBy())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new ClaimResponse(claim.getId(), claim.getEntityType(), claim.getEntityId(), name,
                claim.getRequestedBy(), requester.getEmail(), claim.getMessage(), claim.getStatus(), claim.getRequestedAt());
    }
}
