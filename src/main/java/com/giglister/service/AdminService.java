package com.giglister.service;

import com.giglister.domain.Band;
import com.giglister.domain.Event;
import com.giglister.domain.EventSeries;
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
import com.giglister.dto.admin.AdminEventSeriesListItem;
import com.giglister.dto.admin.AdminLocationListItem;
import com.giglister.dto.admin.AdminUserResponse;
import com.giglister.dto.admin.DuplicatePair;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.BandRepository;
import com.giglister.repository.ClaimRepository;
import com.giglister.repository.EventRepository;
import com.giglister.repository.EventSeriesRepository;
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
    private final EventSeriesRepository eventSeriesRepository;

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
    public Page<AdminBandListItem> listAdminBands(EntityStatus status, String query, String sort, Pageable pageable) {
        List<Band> matches = bandRepository.findAll().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> matchesQuery(b.getName(), query))
                .sorted(completenessOrElse(sort, this::bandCompleteness, Comparator.comparing(Band::getName, String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return paginate(matches, pageable)
                .map(b -> new AdminBandListItem(b.getId(), b.getName(), b.getCity(), b.getStatus(), bandCompleteness(b)));
    }

    public Page<AdminLocationListItem> listAdminLocations(EntityStatus status, String query, String sort, Pageable pageable) {
        List<Location> matches = locationRepository.findAll().stream()
                .filter(l -> status == null || l.getStatus() == status)
                .filter(l -> matchesQuery(l.getName(), query))
                .sorted(completenessOrElse(sort, this::locationCompleteness, Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return paginate(matches, pageable)
                .map(l -> new AdminLocationListItem(l.getId(), l.getName(), l.getCity(), l.getStatus(), locationCompleteness(l)));
    }

    /** `sort` is "completeness_asc" (least complete first - the most actionable order for
     * an admin) or "completeness_desc"; anything else (including absent) keeps each list's
     * own default order. Shared across bands/locations/events since the concept - and the
     * two accepted values - are identical, only which completeness function applies differs. */
    private <T> Comparator<T> completenessOrElse(String sort, java.util.function.ToIntFunction<T> completeness, Comparator<T> defaultOrder) {
        if ("completeness_asc".equals(sort)) {
            return Comparator.comparingInt(completeness);
        }
        if ("completeness_desc".equals(sort)) {
            return Comparator.comparingInt(completeness).reversed();
        }
        return defaultOrder;
    }

    /** How much of a band's optional profile data is filled in, as a 0-100 percentage for
     * the admin overview - `name` is excluded since it's always required, so every band
     * would otherwise start above 0%. Images are included here (unlike BandService's
     * isComplete(), which gates GPT-skill auto-publish and deliberately ignores them since
     * a GPT skill can rarely source them) because this is about what a human admin still
     * has left to do, and a missing logo/photo is exactly that. */
    private int bandCompleteness(Band band) {
        String[] fields = {band.getCity(), band.getCountry(), band.getShortDescription(),
                band.getWebsite(), band.getLogoUrl(), band.getTitleImageUrl()};
        int filled = (int) java.util.Arrays.stream(fields).filter(this::notBlank).count();
        if (band.getGenres() != null && !band.getGenres().isEmpty()) {
            filled++;
        }
        return Math.round(100f * filled / (fields.length + 1));
    }

    /** Same idea as bandCompleteness, for a location - `name`/`city` are excluded since
     * both are always required. Coordinates count as one field: they're usually filled in
     * automatically by geocoding, but a location whose address never resolved still needs
     * an admin to notice and fix it. */
    private int locationCompleteness(Location location) {
        String[] fields = {location.getAddress(), location.getPostalCode(), location.getCountry(),
                location.getWebsite(), location.getLogoUrl(), location.getTitleImageUrl()};
        int filled = (int) java.util.Arrays.stream(fields).filter(this::notBlank).count();
        if (location.getLatitude() != null && location.getLongitude() != null) {
            filled++;
        }
        return Math.round(100f * filled / (fields.length + 1));
    }

    /** Same idea, for an event - `date`, `location` and the line-up (`bands`) are excluded
     * since they're always required at creation. `title` is deliberately excluded too:
     * per Event.title's own javadoc, most concerts are identified by their line-up, not a
     * title, so a missing one isn't something for an admin to "complete". */
    private int eventCompleteness(Event event) {
        int total = 4;
        int filled = 0;
        if (event.getStartTime() != null) filled++;
        if (notBlank(event.getDescription())) filled++;
        if (notBlank(event.getTicketUrl())) filled++;
        if (notBlank(event.getTitleImageUrl())) filled++;
        return Math.round(100f * filled / total);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    public Page<AdminEventSeriesListItem> listAdminEventSeries(String query, Pageable pageable) {
        List<EventSeries> matches = eventSeriesRepository.findAll().stream()
                .filter(s -> matchesQuery(s.getName(), query))
                .sorted(Comparator.comparing(EventSeries::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return paginate(matches, pageable)
                .map(s -> new AdminEventSeriesListItem(s.getId(), s.getName(),
                        eventRepository.findByEventSeriesIdOrderByDateAscStartTimeAsc(s.getId()).size()));
    }

    public Page<AdminEventListItem> listAdminEvents(EventStatus status, String query, String sort, Pageable pageable) {
        List<Event> matches = eventRepository.findAll().stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> matchesQuery(e.getTitle(), query) || matchesEventBandQuery(e, query))
                .sorted(completenessOrElse(sort, this::eventCompleteness, Comparator.comparing(Event::getDate).reversed()))
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
        return new AdminEventListItem(event.getId(), event.getDate(), event.getTitle(), locationName, bandNames,
                event.getStatus(), eventCompleteness(event));
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
