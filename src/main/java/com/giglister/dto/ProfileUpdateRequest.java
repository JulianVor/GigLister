package com.giglister.dto;

public record ProfileUpdateRequest(
        String username,
        String homeCity,
        Double homeLatitude,
        Double homeLongitude,
        Integer radiusKm
) {
}
