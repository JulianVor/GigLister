package com.giglister.web;

import com.giglister.domain.Claim;
import com.giglister.domain.EntityMerge;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.dto.admin.AdminBandListItem;
import com.giglister.dto.admin.AdminCreateUserRequest;
import com.giglister.dto.admin.AdminCreateUserResponse;
import com.giglister.dto.admin.AdminDashboardResponse;
import com.giglister.dto.admin.AdminEventListItem;
import com.giglister.dto.admin.AdminEventSeriesListItem;
import com.giglister.dto.admin.AdminLocationListItem;
import com.giglister.dto.admin.AdminUserResponse;
import com.giglister.dto.admin.ClaimResponse;
import com.giglister.dto.admin.DuplicatePair;
import com.giglister.dto.admin.MergeRequest;
import com.giglister.dto.admin.RejectDuplicateRequest;
import com.giglister.dto.MeResponse;
import com.giglister.dto.submission.RejectSubmissionRequest;
import com.giglister.dto.submission.SubmissionResponse;
import com.giglister.dto.submission.SubmissionUpdateRequest;
import com.giglister.security.CurrentUser;
import com.giglister.service.AdminService;
import com.giglister.service.BandService;
import com.giglister.service.ClaimService;
import com.giglister.service.DataTransferService;
import com.giglister.service.LocationService;
import com.giglister.service.MergeService;
import com.giglister.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Platform-admin data maintenance center: pending claims, drafts, likely
 * duplicates, and merging. All endpoints require ROLE_ADMIN (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ClaimService claimService;
    private final MergeService mergeService;
    private final SubmissionService submissionService;
    private final LocationService locationService;
    private final BandService bandService;
    private final DataTransferService dataTransferService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    /** Every row in every table, as one JSON file - see DataTransferService. */
    @GetMapping("/data/export")
    public ResponseEntity<DataTransferService.DataExport> exportData() {
        DataTransferService.DataExport data = dataTransferService.exportAll();
        String filename = "giglister-export-" + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    /** Replaces every row in every table with what's in the uploaded export - see
     * DataTransferService.importAll for why this needs request.confirm() to exactly
     * match CONFIRMATION_PHRASE. */
    @PostMapping("/data/import")
    public DataTransferService.ImportResult importData(@RequestBody DataTransferService.ImportRequest request) {
        return dataTransferService.importAll(request);
    }

    @GetMapping("/duplicates")
    public List<DuplicatePair> duplicates() {
        return adminService.possibleDuplicates();
    }

    /** "Kein Duplikat" - see AdminService.rejectDuplicate. Distinct from /merge: this pair
     * stays as two separate entities, just stops being suggested again. */
    @PostMapping("/duplicates/reject")
    public ResponseEntity<Void> rejectDuplicate(@Valid @RequestBody RejectDuplicateRequest request) {
        adminService.rejectDuplicate(request.entityType(), request.firstId(), request.secondId(), CurrentUser.requireId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/claims")
    public List<ClaimResponse> claims() {
        return claimService.pending().stream().map(claimService::toResponse).toList();
    }

    @PostMapping("/claims/{id}/approve")
    public ClaimResponse approve(@PathVariable Long id) {
        Claim claim = claimService.approve(id, CurrentUser.requireId());
        return claimService.toResponse(claim);
    }

    @PostMapping("/claims/{id}/reject")
    public ClaimResponse reject(@PathVariable Long id) {
        Claim claim = claimService.reject(id, CurrentUser.requireId());
        return claimService.toResponse(claim);
    }

    @PostMapping("/merge")
    public EntityMerge merge(@Valid @RequestBody MergeRequest request) {
        return mergeService.merge(request.entityType(), request.sourceEntityId(), request.targetEntityId(), CurrentUser.requireId());
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) String q) {
        return adminService.listUsers(q);
    }

    /** "Details ansehen" - see AdminService.userDetail. */
    @GetMapping("/users/{id}")
    public MeResponse userDetail(@PathVariable Long id) {
        return adminService.userDetail(id);
    }

    @PostMapping("/users")
    public ResponseEntity<AdminCreateUserResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
    }

    @PostMapping("/users/{id}/promote")
    public AdminUserResponse promote(@PathVariable Long id) {
        return adminService.setPlatformAdmin(id, true, CurrentUser.requireId());
    }

    @PostMapping("/users/{id}/demote")
    public AdminUserResponse demote(@PathVariable Long id) {
        return adminService.setPlatformAdmin(id, false, CurrentUser.requireId());
    }

    /** `sort` is "completeness_asc"/"completeness_desc" (see AdminService.completenessOrElse)
     * or omitted for the default name order. */
    @GetMapping("/bands")
    public Page<AdminBandListItem> bands(
            @RequestParam(required = false) EntityStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminBands(status, q, sort, PageRequest.of(page, size));
    }

    /** "Alle auf Vollständigkeit setzen" on the Bands overview - see BandService.publishAllComplete. */
    @PostMapping("/bands/publish-complete")
    public BandService.PublishCompleteResult publishCompleteBands() {
        return bandService.publishAllComplete();
    }

    /** One-off backfill for locations saved before geocoding existed - see
     * LocationService.backfillMissingCoordinates. Synchronous and rate-limited
     * (~1/sec), so this can take a while on a large backlog; fine for the admin
     * panel's small, occasional data-maintenance use, not meant to be automated. */
    @PostMapping("/locations/geocode-missing")
    public LocationService.BackfillResult geocodeMissingLocations() {
        return locationService.backfillMissingCoordinates();
    }

    /** "Alle auf Vollständigkeit setzen" on the Orte overview - see LocationService.publishAllComplete. */
    @PostMapping("/locations/publish-complete")
    public LocationService.PublishCompleteResult publishCompleteLocations() {
        return locationService.publishAllComplete();
    }

    @GetMapping("/locations")
    public Page<AdminLocationListItem> locations(
            @RequestParam(required = false) EntityStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminLocations(status, q, sort, PageRequest.of(page, size));
    }

    @GetMapping("/events")
    public Page<AdminEventListItem> events(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminEvents(status, q, sort, PageRequest.of(page, size));
    }

    @GetMapping("/festivals")
    public Page<AdminEventSeriesListItem> eventSeries(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminEventSeries(q, PageRequest.of(page, size));
    }

    @GetMapping("/submissions")
    public List<SubmissionResponse> submissions(@RequestParam(required = false) SubmissionStatus status) {
        return submissionService.list(status).stream().map(submissionService::toResponse).toList();
    }

    @GetMapping("/submissions/{id}")
    public SubmissionResponse submission(@PathVariable Long id) {
        return submissionService.toResponse(submissionService.getOrThrow(id));
    }

    @PutMapping("/submissions/{id}")
    public SubmissionResponse updateSubmission(@PathVariable Long id, @Valid @RequestBody SubmissionUpdateRequest request) {
        return submissionService.toResponse(submissionService.update(id, request));
    }

    @PostMapping("/submissions/{id}/approve")
    public SubmissionResponse approveSubmission(@PathVariable Long id) {
        return submissionService.toResponse(submissionService.approve(id, CurrentUser.requireId()));
    }

    @PostMapping("/submissions/{id}/reject")
    public SubmissionResponse rejectSubmission(@PathVariable Long id, @RequestBody(required = false) RejectSubmissionRequest body) {
        String reason = body != null ? body.reason() : null;
        return submissionService.toResponse(submissionService.reject(id, CurrentUser.requireId(), reason));
    }
}
