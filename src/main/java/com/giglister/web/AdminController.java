package com.giglister.web;

import com.giglister.domain.Claim;
import com.giglister.domain.EntityMerge;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.dto.admin.AdminBandListItem;
import com.giglister.dto.admin.AdminDashboardResponse;
import com.giglister.dto.admin.AdminEventListItem;
import com.giglister.dto.admin.AdminLocationListItem;
import com.giglister.dto.admin.AdminUserResponse;
import com.giglister.dto.admin.ClaimResponse;
import com.giglister.dto.admin.DuplicatePair;
import com.giglister.dto.admin.MergeRequest;
import com.giglister.dto.submission.RejectSubmissionRequest;
import com.giglister.dto.submission.SubmissionResponse;
import com.giglister.dto.submission.SubmissionUpdateRequest;
import com.giglister.security.CurrentUser;
import com.giglister.service.AdminService;
import com.giglister.service.ClaimService;
import com.giglister.service.LocationService;
import com.giglister.service.MergeService;
import com.giglister.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/duplicates")
    public List<DuplicatePair> duplicates() {
        return adminService.possibleDuplicates();
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

    @PostMapping("/users/{id}/promote")
    public AdminUserResponse promote(@PathVariable Long id) {
        return adminService.setPlatformAdmin(id, true, CurrentUser.requireId());
    }

    @PostMapping("/users/{id}/demote")
    public AdminUserResponse demote(@PathVariable Long id) {
        return adminService.setPlatformAdmin(id, false, CurrentUser.requireId());
    }

    @GetMapping("/bands")
    public Page<AdminBandListItem> bands(
            @RequestParam(required = false) EntityStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminBands(status, q, PageRequest.of(page, size));
    }

    /** One-off backfill for locations saved before geocoding existed - see
     * LocationService.backfillMissingCoordinates. Synchronous and rate-limited
     * (~1/sec), so this can take a while on a large backlog; fine for the admin
     * panel's small, occasional data-maintenance use, not meant to be automated. */
    @PostMapping("/locations/geocode-missing")
    public LocationService.BackfillResult geocodeMissingLocations() {
        return locationService.backfillMissingCoordinates();
    }

    @GetMapping("/locations")
    public Page<AdminLocationListItem> locations(
            @RequestParam(required = false) EntityStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminLocations(status, q, PageRequest.of(page, size));
    }

    @GetMapping("/events")
    public Page<AdminEventListItem> events(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.listAdminEvents(status, q, PageRequest.of(page, size));
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
