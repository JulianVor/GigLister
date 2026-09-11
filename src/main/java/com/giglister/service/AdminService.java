package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.Event;
import com.giglister.domain.Location;
import com.giglister.domain.User;
import com.giglister.domain.enums.ClaimStatus;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.dto.admin.AdminBandListItem;
import com.giglister.dto.admin.AdminDashboardResponse;
import com.giglister.dto.admin.AdminEventListItem;
import com.giglister.dto.admin.AdminLocationListItem;
import com.giglister.dto.admin.AdminUserResponse;
import com.giglister.dto.admin.DuplicatePair;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandRepository;
import com.giglister.repository.ClaimRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.LocationRepository;
import com.giglister.repository.SubmissionRepository;
import com.giglister.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClaimRepository claimRepository;
    private final BandRepository bandRepository;
    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    private static final List<EntityStatus> NEEDS_ATTENTION = List.of(EntityStatus.STUB, EntityStatus.DRAFT);

    public AdminDashboardResponse dashboard() {
        long openClaims = claimRepository.findByStatus(ClaimStatus.PENDING).size();
        // STUB, not just DRAFT - a stub (e.g. created inline while adding an event)
        // needs just as much admin attention as a draft, often more.
        long bandsNeedingAttention = bandRepository.countByStatusIn(NEEDS_ATTENTION);
        long locationsNeedingAttention = locationRepository.countByStatusIn(NEEDS_ATTENTION);
        long possibleDuplicates = possibleDuplicates().size();
        long pendingSubmissions = submissionRepository.findByStatusOrderBySubmittedAtDesc(SubmissionStatus.PENDING).size();
        return new AdminDashboardResponse(openClaims, bandsNeedingAttention, locationsNeedingAttention,
                possibleDuplicates, pendingSubmissions);
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

    public List<AdminUserResponse> listUsers(String query) {
        List<User> users = (query == null || query.isBlank())
                ? userRepository.findAll()
                : userRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(query, query);
        return users.stream()
                .sorted(Comparator.comparing(User::getEmail))
                .map(u -> new AdminUserResponse(u.getId(), u.getEmail(), u.getUsername(), u.isPlatformAdmin()))
                .toList();
    }

    @Transactional
    public AdminUserResponse setPlatformAdmin(Long userId, boolean platformAdmin, Long actingUserId) {
        if (!platformAdmin && userId.equals(actingUserId)) {
            throw new BadRequestException("You cannot remove your own admin rights");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        user.setPlatformAdmin(platformAdmin);
        userRepository.save(user);
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getUsername(), user.isPlatformAdmin());
    }

    /**
     * Full overview of every Band, regardless of status - the "STUB/DRAFT items
     * that still need completing" list the public /bands endpoint deliberately
     * hides (it only ever returns PUBLISHED).
     */
    public Page<AdminBandListItem> listAdminBands(EntityStatus status, String query, Pageable pageable) {
        List<Band> matches = bandRepository.findAll().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> matchesQuery(b.getName(), query))
                .sorted(Comparator.comparing(Band::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return paginate(matches, pageable)
                .map(b -> new AdminBandListItem(b.getId(), b.getName(), b.getCity(), b.getStatus()));
    }

    public Page<AdminLocationListItem> listAdminLocations(EntityStatus status, String query, Pageable pageable) {
        List<Location> matches = locationRepository.findAll().stream()
                .filter(l -> status == null || l.getStatus() == status)
                .filter(l -> matchesQuery(l.getName(), query))
                .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return paginate(matches, pageable)
                .map(l -> new AdminLocationListItem(l.getId(), l.getName(), l.getCity(), l.getStatus()));
    }

    public Page<AdminEventListItem> listAdminEvents(EventStatus status, String query, Pageable pageable) {
        List<Event> matches = eventRepository.findAll().stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> matchesQuery(e.getTitle(), query) || matchesEventBandQuery(e, query))
                .sorted(Comparator.comparing(Event::getDate).reversed())
                .toList();
        return paginate(matches, pageable).map(this::toAdminEventListItem);
    }

    private AdminEventListItem toAdminEventListItem(Event event) {
        String locationName = locationRepository.findById(event.getLocationId())
                .map(Location::getName)
                .orElse("(gelöscht)");
        List<String> bandNames = event.getBandIds().stream()
                .map(id -> bandRepository.findById(id).map(Band::getName).orElse("(gelöscht)"))
                .toList();
        return new AdminEventListItem(event.getId(), event.getDate(), event.getTitle(), locationName, bandNames, event.getStatus());
    }

    private boolean matchesQuery(String value, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return value != null && TextNormalizer.normalize(value).contains(TextNormalizer.normalize(query));
    }

    private boolean matchesEventBandQuery(Event event, String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return event.getBandIds().stream()
                .anyMatch(id -> bandRepository.findById(id).map(Band::getName).filter(name -> matchesQuery(name, query)).isPresent());
    }

    private <T> Page<T> paginate(List<T> items, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }
}
