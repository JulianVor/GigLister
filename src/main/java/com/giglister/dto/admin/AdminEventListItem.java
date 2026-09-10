package com.giglister.dto.admin;

import com.giglister.domain.enums.EventStatus;

import java.time.LocalDate;
import java.util.List;

public record AdminEventListItem(
        Long id,
        LocalDate date,
        String title,
        String locationName,
        List<String> bandNames,
        EventStatus status
) {
}
