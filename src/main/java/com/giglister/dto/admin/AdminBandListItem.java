package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityStatus;

public record AdminBandListItem(
        Long id,
        String name,
        String city,
        EntityStatus status,
        /** 0-100, see AdminService.bandCompleteness - how much of the optional profile
         * data (beyond the always-required name) is filled in. */
        int completeness
) {
}
