package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.Location;
import com.giglister.domain.enums.ClaimStatus;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.dto.admin.AdminDashboardResponse;
import com.giglister.dto.admin.DuplicatePair;
import com.giglister.repository.BandRepository;
import com.giglister.repository.ClaimRepository;
import com.giglister.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClaimRepository claimRepository;
    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;

    public AdminDashboardResponse dashboard() {
        long openClaims = claimRepository.findByStatus(ClaimStatus.PENDING).size();
        long bandDrafts = bandRepository.countByStatus(EntityStatus.DRAFT);
        long locationDrafts = locationRepository.countByStatus(EntityStatus.DRAFT);
        long possibleDuplicates = possibleDuplicates().size();
        return new AdminDashboardResponse(openClaims, bandDrafts, locationDrafts, possibleDuplicates);
    }

    /** Cheap O(n^2) pairwise scan over non-archived bands/locations - fine for V1's data volume. */
    public List<DuplicatePair> possibleDuplicates() {
        List<DuplicatePair> result = new ArrayList<>();
        List<Band> bands = bandRepository.findByStatusIn(
                List.of(EntityStatus.STUB, EntityStatus.DRAFT, EntityStatus.PUBLISHED));
        for (int i = 0; i < bands.size(); i++) {
            for (int j = i + 1; j < bands.size(); j++) {
                double sim = TextNormalizer.similarity(bands.get(i).getName(), bands.get(j).getName());
                boolean sameCity = sameCity(bands.get(i).getCity(), bands.get(j).getCity());
                if (sim >= 0.8 || (sim >= 0.6 && sameCity)) {
                    result.add(new DuplicatePair(EntityType.BAND,
                            bands.get(i).getId(), bands.get(i).getName(),
                            bands.get(j).getId(), bands.get(j).getName(),
                            bands.get(i).getCity(), sim));
                }
            }
        }
        List<Location> locations = locationRepository.findByStatusIn(
                List.of(EntityStatus.STUB, EntityStatus.DRAFT, EntityStatus.PUBLISHED));
        for (int i = 0; i < locations.size(); i++) {
            for (int j = i + 1; j < locations.size(); j++) {
                double sim = TextNormalizer.similarity(locations.get(i).getName(), locations.get(j).getName());
                boolean sameCity = sameCity(locations.get(i).getCity(), locations.get(j).getCity());
                if (sim >= 0.8 || (sim >= 0.6 && sameCity)) {
                    result.add(new DuplicatePair(EntityType.LOCATION,
                            locations.get(i).getId(), locations.get(i).getName(),
                            locations.get(j).getId(), locations.get(j).getName(),
                            locations.get(i).getCity(), sim));
                }
            }
        }
        return result;
    }

    private boolean sameCity(String a, String b) {
        return a != null && b != null && TextNormalizer.normalize(a).equals(TextNormalizer.normalize(b));
    }
}
