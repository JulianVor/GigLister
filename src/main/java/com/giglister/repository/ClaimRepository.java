package com.giglister.repository;

import com.giglister.domain.Claim;
import com.giglister.domain.enums.ClaimStatus;
import com.giglister.domain.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByStatus(ClaimStatus status);

    Optional<Claim> findByEntityTypeAndEntityIdAndRequestedByAndStatus(
            EntityType entityType, Long entityId, Long requestedBy, ClaimStatus status);
}
