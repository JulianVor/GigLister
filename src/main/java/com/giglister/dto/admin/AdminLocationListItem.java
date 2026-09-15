package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityStatus;

public record AdminLocationListItem(
        Long id,
        String name,
        String city,
        EntityStatus status,
        /** 0-100, see AdminService.locationCompleteness - how much of the optional profile
         * data (beyond the always-required name/city) is filled in. */
        int completeness
) {
}
