package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityType;
import com.giglister.dto.admin.DuplicateCandidate;
import com.giglister.repository.BandRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Robust normalized-text duplicate detection for Bands and Locations, per the
 * "no empty profile pages, no duplicates" principle: before a new Band/Location
 * is created, existing candidates are surfaced ("Meintest du?").
 */
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private static final double SIMILARITY_THRESHOLD = 0.55;
    private static final int MAX_CANDIDATES = 5;

    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;

    public List<DuplicateCandidate> findBandCandidates(String name, String city) {
        return bandRepository.findAll().stream()
                .map(b -> toCandidate(b, name, city))
                .filter(c -> c.similarity() >= SIMILARITY_THRESHOLD)
                .sorted(Comparator.comparingDouble(DuplicateCandidate::similarity).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    public List<DuplicateCandidate> findLocationCandidates(String name, String city) {
        return locationRepository.findAll().stream()
                .map(l -> toCandidate(l, name, city))
                .filter(c -> c.similarity() >= SIMILARITY_THRESHOLD)
                .sorted(Comparator.comparingDouble(DuplicateCandidate::similarity).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    private DuplicateCandidate toCandidate(Band band, String name, String city) {
        double sim = score(band.getName(), band.getCity(), name, city);
        return new DuplicateCandidate(EntityType.BAND, band.getId(), band.getName(), band.getCity(), sim);
    }

    private DuplicateCandidate toCandidate(Location location, String name, String city) {
        double sim = score(location.getName(), location.getCity(), name, city);
        return new DuplicateCandidate(EntityType.LOCATION, location.getId(), location.getName(), location.getCity(), sim);
    }

    private double score(String existingName, String existingCity, String queryName, String queryCity) {
        double nameSim = TextNormalizer.similarity(existingName, queryName);
        boolean sameCity = queryCity != null && existingCity != null
                && TextNormalizer.normalize(existingCity).equals(TextNormalizer.normalize(queryCity));
        // A same-city match boosts confidence; a city mismatch when both are known pulls it down.
        if (queryCity != null && existingCity != null) {
            return sameCity ? Math.min(1.0, nameSim + 0.15) : nameSim * 0.7;
        }
        return nameSim;
    }
}
