package com.giglister.dto.admin;

public record AdminDashboardResponse(
        long openClaims,
        long bandsNeedingAttention,
        long locationsNeedingAttention,
        long possibleDuplicates
) {
}
