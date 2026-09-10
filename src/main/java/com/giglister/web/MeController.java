package com.giglister.web;

import com.giglister.dto.MeResponse;
import com.giglister.dto.ProfileUpdateRequest;
import com.giglister.dto.band.BandResponse;
import com.giglister.dto.common.EventSummary;
import com.giglister.security.CurrentUser;
import com.giglister.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

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
}
