package com.giglister.web;

import com.giglister.domain.Claim;
import com.giglister.domain.Location;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.ClaimRequest;
import com.giglister.dto.PermissionRequest;
import com.giglister.dto.PermissionResponse;
import com.giglister.dto.StatusUpdateRequest;
import com.giglister.dto.admin.ClaimResponse;
import com.giglister.dto.admin.DuplicateCandidate;
import com.giglister.dto.location.LocationCreateRequest;
import com.giglister.dto.location.LocationListItem;
import com.giglister.dto.location.LocationResponse;
import com.giglister.dto.location.LocationUpdateRequest;
import com.giglister.exception.NotFoundException;
import com.giglister.security.CurrentUser;
import com.giglister.service.ClaimService;
import com.giglister.service.DuplicateDetectionService;
import com.giglister.service.LocationService;
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
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final PermissionService permissionService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ClaimService claimService;

    @GetMapping
    public Page<LocationListItem> list(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return locationService.listPublished(city, PageRequest.of(page, size)).map(locationService::toListItem);
    }

    @GetMapping("/duplicates")
    public List<DuplicateCandidate> duplicates(@RequestParam String name, @RequestParam(required = false) String city) {
        return duplicateDetectionService.findLocationCandidates(name, city);
    }

    @GetMapping("/{id}")
    public LocationResponse get(@PathVariable Long id) {
        Location location = locationService.getOrThrow(id);
        assertVisible(location);
        return locationService.toResponse(location);
    }

    /**
     * PUBLISHED locations are visible to everyone. A STUB/DRAFT location has no
     * real public profile yet, so it's hidden from anonymous visitors - but any
     * logged-in user can still reach it, otherwise nobody could ever discover
     * and claim a venue they just saw referenced in an event.
     */
    private void assertVisible(Location location) {
        if (location.getStatus() == EntityStatus.PUBLISHED) {
            return;
        }
        if (CurrentUser.getOrNull() == null) {
            throw new NotFoundException("Location " + location.getId() + " not found");
        }
    }

    @PostMapping
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationCreateRequest request) {
        Location location = locationService.create(request, CurrentUser.requireId());
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.toResponse(location));
    }

    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable Long id, @Valid @RequestBody LocationUpdateRequest request) {
        var user = CurrentUser.require();
        return locationService.toResponse(locationService.update(id, request, user.getId(), user.isPlatformAdmin()));
    }

    @PatchMapping("/{id}/status")
    public LocationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        var user = CurrentUser.require();
        return locationService.toResponse(locationService.updateStatus(id, request.status(), user.getId(), user.isPlatformAdmin()));
    }

    @GetMapping("/{id}/permissions")
    public List<PermissionResponse> permissions(@PathVariable Long id) {
        var user = CurrentUser.require();
        permissionService.require(user.getId(), user.isPlatformAdmin(), EntityType.LOCATION, id, PermissionLevel.MANAGE);
        return permissionService.listHolders(EntityType.LOCATION, id);
    }

    @PostMapping("/{id}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantPermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        var user = CurrentUser.require();
        permissionService.require(user.getId(), user.isPlatformAdmin(), EntityType.LOCATION, id, PermissionLevel.MANAGE);
        permissionService.grant(EntityType.LOCATION, id, request.userId(), request.permission(), user.getId());
    }

    @DeleteMapping("/{id}/permissions/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(@PathVariable Long id, @PathVariable Long userId) {
        var user = CurrentUser.require();
        permissionService.require(user.getId(), user.isPlatformAdmin(), EntityType.LOCATION, id, PermissionLevel.MANAGE);
        permissionService.revoke(EntityType.LOCATION, id, userId);
    }

    @PostMapping("/{id}/claim")
    public ClaimResponse claim(@PathVariable Long id, @RequestBody(required = false) ClaimRequest request) {
        String message = request != null ? request.message() : null;
        Claim claim = claimService.request(EntityType.LOCATION, id, CurrentUser.requireId(), message);
        return claimService.toResponse(claim);
    }
}
