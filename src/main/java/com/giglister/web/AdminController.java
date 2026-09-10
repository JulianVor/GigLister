package com.giglister.web;

import com.giglister.domain.Claim;
import com.giglister.domain.EntityMerge;
import com.giglister.dto.admin.AdminDashboardResponse;
import com.giglister.dto.admin.AdminUserResponse;
import com.giglister.dto.admin.ClaimResponse;
import com.giglister.dto.admin.DuplicatePair;
import com.giglister.dto.admin.MergeRequest;
import com.giglister.security.CurrentUser;
import com.giglister.service.AdminService;
import com.giglister.service.ClaimService;
import com.giglister.service.MergeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
