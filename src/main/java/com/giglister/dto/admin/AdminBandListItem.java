package com.giglister.dto.admin;

import com.giglister.domain.enums.EntityStatus;

public record AdminBandListItem(
        Long id,
        String name,
        String city,
        EntityStatus status
) {
}
