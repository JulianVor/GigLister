package com.giglister.web;

import com.giglister.dto.MeResponse;
import com.giglister.dto.ProfileUpdateRequest;
import com.giglister.security.CurrentUser;
import com.giglister.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
