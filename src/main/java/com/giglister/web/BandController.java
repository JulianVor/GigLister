package com.giglister.web;

import com.giglister.domain.Band;
import com.giglister.domain.Claim;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.ClaimRequest;
import com.giglister.dto.PermissionRequest;
import com.giglister.dto.PermissionResponse;
import com.giglister.dto.StatusUpdateRequest;
import com.giglister.dto.admin.ClaimResponse;
import com.giglister.dto.admin.DuplicateCandidate;
import com.giglister.dto.band.BandCreateRequest;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.band.BandUpdateRequest;
import com.giglister.exception.NotFoundException;
import com.giglister.security.AppUserPrincipal;
import com.giglister.security.CurrentUser;
import com.giglister.service.BandService;
import com.giglister.service.ClaimService;
import com.giglister.service.DuplicateDetectionService;
import com.giglister.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bands")
@RequiredArgsConstructor
public class BandController {

    private final BandService bandService;
    private final PermissionService permissionService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ClaimService claimService;

    @GetMapping
    public Page<BandResponse> list(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return bandService.listPublished(city, PageRequest.of(page, size)).map(bandService::toResponse);
    }

    @GetMapping("/duplicates")
    public List<DuplicateCandidate> duplicates(@RequestParam String name, @RequestParam(required = false) String city) {
        return duplicateDetectionService.findBandCandidates(name, city);
    }

    @GetMapping("/{id}")
    public BandResponse get(@PathVariable Long id) {
        Band band = bandService.getOrThrow(id);
        assertVisible(band);
        return bandService.toResponse(band);
    }

    private void assertVisible(Band band) {
        if (band.getStatus() == EntityStatus.PUBLISHED) {
            return;
        }
        AppUserPrincipal user = CurrentUser.getOrNull();
        boolean allowed = user != null && (user.isPlatformAdmin()
                || permissionService.has(user.getId(), false, EntityType.BAND, band.getId(), PermissionLevel.EDIT));
        if (!allowed) {
            throw new NotFoundException("Band " + band.getId() + " not found");
        }
    }

    @PostMapping
    public ResponseEntity<BandResponse> create(@Valid @RequestBody BandCreateRequest request) {
        Band band = bandService.create(request, CurrentUser.requireId());
        return ResponseEntity.status(HttpStatus.CREATED).body(bandService.toResponse(band));
    }

    @PutMapping("/{id}")
    public BandResponse update(@PathVariable Long id, @Valid @RequestBody BandUpdateRequest request) {
        var user = CurrentUser.require();
        return bandService.toResponse(bandService.update(id, request, user.getId(), user.isPlatformAdmin()));
    }

    @PatchMapping("/{id}/status")
    public BandResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        var user = CurrentUser.require();
        return bandService.toResponse(bandService.updateStatus(id, request.status(), user.getId(), user.isPlatformAdmin()));
    }

    @GetMapping("/{id}/permissions")
    public List<PermissionResponse> permissions(@PathVariable Long id) {
        var user = CurrentUser.require();
        permissionService.require(user.getId(), user.isPlatformAdmin(), EntityType.BAND, id, PermissionLevel.MANAGE);
        return permissionService.listHolders(EntityType.BAND, id);
    }

    @PostMapping("/{id}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantPermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        var user = CurrentUser.require();
        permissionService.require(user.getId(), user.isPlatformAdmin(), EntityType.BAND, id, PermissionLevel.MANAGE);
        permissionService.grant(EntityType.BAND, id, request.userId(), request.permission(), user.getId());
    }

    @DeleteMapping("/{id}/permissions/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(@PathVariable Long id, @PathVariable Long userId) {
        var user = CurrentUser.require();
        permissionService.require(user.getId(), user.isPlatformAdmin(), EntityType.BAND, id, PermissionLevel.MANAGE);
        permissionService.revoke(EntityType.BAND, id, userId);
    }

    @PostMapping("/{id}/claim")
    public ClaimResponse claim(@PathVariable Long id, @RequestBody(required = false) ClaimRequest request) {
        String message = request != null ? request.message() : null;
        Claim claim = claimService.request(EntityType.BAND, id, CurrentUser.requireId(), message);
        return claimService.toResponse(claim);
    }

    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long id) {
        bandService.follow(id, CurrentUser.requireId());
    }

    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long id) {
        bandService.unfollow(id, CurrentUser.requireId());
    }
}
