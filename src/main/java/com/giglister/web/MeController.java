package com.giglister.web;

import com.giglister.dto.ChangePasswordRequest;
import com.giglister.dto.DeviceTokenRequest;
import com.giglister.dto.MeResponse;
import com.giglister.dto.ProfileUpdateRequest;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.security.CurrentUser;
import com.giglister.service.AuthService;
import com.giglister.service.PushNotificationService;
import com.giglister.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final PushNotificationService pushNotificationService;
    private final AuthService authService;

    @GetMapping
    public MeResponse me() {
        return userService.toMeResponse(userService.getOrThrow(CurrentUser.requireId()));
    }

    @PutMapping
    public MeResponse updateProfile(@RequestBody ProfileUpdateRequest request) {
        return userService.toMeResponse(userService.updateProfile(CurrentUser.requireId(), request));
    }

    /** "Meine Bands": every band the current user holds EDIT/MANAGE on. */
    @GetMapping("/bands")
    public List<BandResponse> myBands() {
        return userService.myManagedBands(CurrentUser.requireId());
    }

    /** "Meine Veranstaltungen": upcoming events across all of the user's bands, band-übergreifend. */
    @GetMapping("/events")
    public List<EventSummary> myEvents() {
        return userService.myBandEvents(CurrentUser.requireId());
    }

    /** Registers (or reassigns, if already registered to a different account) this
     * device's Firebase token for push notifications - called by the Android app once
     * it has a token and knows who's logged in. */
    @PostMapping("/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(@Valid @RequestBody DeviceTokenRequest request) {
        pushNotificationService.registerToken(CurrentUser.requireId(), request.token());
    }

    /** Called on logout, so a signed-out device stops receiving another account's pushes. */
    @DeleteMapping("/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDeviceToken(@Valid @RequestBody DeviceTokenRequest request) {
        pushNotificationService.unregisterToken(request.token());
    }

    /** Changing a known password while logged in - no email round-trip needed, unlike
     * /api/auth/reset-password, since knowing the current password already proves it's
     * really the account owner. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.requireId(), request.currentPassword(), request.newPassword());
    }
}
