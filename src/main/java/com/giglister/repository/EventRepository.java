package com.giglister.repository;

import com.giglister.domain.Event;
import com.giglister.domain.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            EventStatus status, LocalDate from, Pageable pageable);

    Page<Event> findByStatusAndDateBetweenOrderByDateAscStartTimeAsc(
            EventStatus status, LocalDate from, LocalDate to, Pageable pageable);

    List<Event> findByStatusAndDateOrderByStartTimeAsc(EventStatus status, LocalDate date);

    List<Event> findByLocationIdAndStatusAndDateGreaterThanEqualOrderByDateAsc(
            Long locationId, EventStatus status, LocalDate from);

    List<Event> findByLocationIdAndStatusAndDateLessThanOrderByDateDesc(
            Long locationId, EventStatus status, LocalDate before);

    List<Event> findByLocationId(Long locationId);

    @Query("select e from Event e where e.status = :status and :bandId member of e.bandIds and e.date >= :from order by e.date asc, e.startTime asc")
    List<Event> findUpcomingForBand(@Param("bandId") Long bandId, @Param("status") EventStatus status, @Param("from") LocalDate from);

    @Query("select e from Event e where :bandId member of e.bandIds")
    List<Event> findByBandId(@Param("bandId") Long bandId);

    /** Cross-band: every upcoming event featuring any of the given bands, deduplicated. */
    @Query("select distinct e from Event e join e.bandIds b where b in :bandIds and e.status = :status and e.date >= :from order by e.date asc, e.startTime asc")
    List<Event> findUpcomingForAnyBand(@Param("bandIds") List<Long> bandIds, @Param("status") EventStatus status, @Param("from") LocalDate from);

    @Query("select e from Event e where e.status = :status and e.date between :from and :to and lower(coalesce(e.title, '')) like lower(concat('%', :q, '%'))")
    List<Event> searchByTitleAndStatus(@Param("q") String query, @Param("status") EventStatus status,
                                        @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e.date as day, count(e) as cnt from Event e where e.status = :status and e.date between :from and :to group by e.date")
    List<DayCount> countByDateBetween(@Param("status") EventStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

    interface DayCount {
        LocalDate getDay();
        long getCnt();
    }
}
