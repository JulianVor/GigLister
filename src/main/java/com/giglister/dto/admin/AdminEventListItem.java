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
        EventStatus status,
        /** 0-100, see AdminService.eventCompleteness - how much of the optional detail
         * data (beyond the always-required date/location/line-up) is filled in. */
        int completeness
) {
}
