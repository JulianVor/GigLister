package com.giglister.repository;

import com.giglister.domain.Band;
import com.giglister.domain.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BandRepository extends JpaRepository<Band, Long> {

    Page<Band> findByStatus(EntityStatus status, Pageable pageable);

    Page<Band> findByStatusAndCityIgnoreCase(EntityStatus status, String city, Pageable pageable);

    @Query("select b from Band b where b.status = :status and lower(b.name) like lower(concat('%', :q, '%'))")
    List<Band> searchByNameAndStatus(@Param("q") String query, @Param("status") EntityStatus status);

    @Query("select b from Band b where lower(b.name) like lower(concat('%', :q, '%'))")
    List<Band> searchByName(@Param("q") String query);

    List<Band> findByStatusIn(List<EntityStatus> statuses);

    long countByStatus(EntityStatus status);
}
