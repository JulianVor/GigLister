package com.giglister.service;

import org.springframework.stereotype.Service;

@Service
public class GeoService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Great-circle distance between two points, in kilometers. */
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public boolean withinRadius(Double lat, Double lon, Double centerLat, Double centerLon, Integer radiusKm) {
        if (centerLat == null || centerLon == null || radiusKm == null) {
            return true;
        }
        if (lat == null || lon == null) {
            return true;
        }
        return distanceKm(lat, lon, centerLat, centerLon) <= radiusKm;
    }
}
