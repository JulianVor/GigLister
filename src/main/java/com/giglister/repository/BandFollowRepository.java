package com.giglister.repository;

import com.giglister.domain.BandFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BandFollowRepository extends JpaRepository<BandFollow, Long> {
    List<BandFollow> findByUserId(Long userId);

    Optional<BandFollow> findByUserIdAndBandId(Long userId, Long bandId);

    void deleteByUserIdAndBandId(Long userId, Long bandId);

    boolean existsByUserIdAndBandId(Long userId, Long bandId);
}
