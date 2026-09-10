package com.giglister.dto;

public record ProfileUpdateRequest(
        String displayName,
        String homeCity,
        Double homeLatitude,
        Double homeLongitude,
        Integer radiusKm
) {
}
