package com.giglister.repository;

import com.giglister.domain.SavedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedEventRepository extends JpaRepository<SavedEvent, Long> {
    List<SavedEvent> findByUserId(Long userId);

    Optional<SavedEvent> findByUserIdAndEventId(Long userId, Long eventId);

    void deleteByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    /** For the "Beliebt in deiner Nähe" fallback when there's no personalization signal yet
     * - one grouped query instead of a save-count lookup per candidate event. */
    @Query("select se.eventId as eventId, count(se) as cnt from SavedEvent se where se.eventId in :eventIds group by se.eventId")
    List<EventSaveCount> countByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventSaveCount {
        Long getEventId();
        long getCnt();
    }
}
