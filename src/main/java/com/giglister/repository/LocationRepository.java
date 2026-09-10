package com.giglister.repository;

import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Page<Location> findByStatus(EntityStatus status, Pageable pageable);

    Page<Location> findByStatusAndCityIgnoreCase(EntityStatus status, String city, Pageable pageable);

    @Query("select l from Location l where l.status = :status and lower(l.name) like lower(concat('%', :q, '%'))")
    List<Location> searchByNameAndStatus(@Param("q") String query, @Param("status") EntityStatus status);

    @Query("select l from Location l where lower(l.name) like lower(concat('%', :q, '%'))")
    List<Location> searchByName(@Param("q") String query);

    List<Location> findByStatusIn(List<EntityStatus> statuses);

    long countByStatus(EntityStatus status);
}
