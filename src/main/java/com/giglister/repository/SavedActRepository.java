package com.giglister.repository;

import com.giglister.domain.SavedAct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedActRepository extends JpaRepository<SavedAct, Long> {
    List<SavedAct> findByUserId(Long userId);

    boolean existsByUserIdAndEventIdAndBandId(Long userId, Long eventId, Long bandId);

    void deleteByUserIdAndEventIdAndBandId(Long userId, Long eventId, Long bandId);
}
