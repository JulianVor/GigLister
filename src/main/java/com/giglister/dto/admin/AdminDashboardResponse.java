package com.giglister.dto.admin;

public record AdminDashboardResponse(
        long openClaims,
        long bandDrafts,
        long locationDrafts,
        long possibleDuplicates
) {
}
