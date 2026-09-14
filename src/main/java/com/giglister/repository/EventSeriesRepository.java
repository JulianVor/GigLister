package com.giglister.repository;

import com.giglister.domain.EventSeries;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSeriesRepository extends JpaRepository<EventSeries, Long> {
}
