package com.giglister.repository;

import com.giglister.domain.BandStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BandStoryRepository extends JpaRepository<BandStory, Long> {
    List<BandStory> findByBandIdAndExpiresAtAfterOrderByCreatedAtAsc(Long bandId, Instant now);

    /** Batch "which of these bands currently have a live story" check (see
     * UserService.toMeResponse) - one query for a whole followedBands list instead of one
     * per band. */
    List<BandStory> findByBandIdInAndExpiresAtAfter(List<Long> bandIds, Instant now);

    void deleteByBandId(Long bandId);
}
