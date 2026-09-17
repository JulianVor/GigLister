package com.giglister.web;

import com.giglister.dto.band.BandStoryCreateRequest;
import com.giglister.dto.band.BandStoryResponse;
import com.giglister.security.CurrentUser;
import com.giglister.service.BandStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bands/{bandId}/stories")
@RequiredArgsConstructor
public class BandStoryController {

    private final BandStoryService bandStoryService;

    /** Public like GET /api/bands/{id} itself (falls under the same GET /api/bands/** matcher
     * in SecurityConfig) - a story is exactly as visible as the band it belongs to. */
    @GetMapping
    public List<BandStoryResponse> list(@PathVariable Long bandId) {
        return bandStoryService.listActive(bandId).stream().map(bandStoryService::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<BandStoryResponse> create(@PathVariable Long bandId, @Valid @RequestBody BandStoryCreateRequest request) {
        var user = CurrentUser.require();
        var story = bandStoryService.create(bandId, request, user.getId(), user.isPlatformAdmin());
        return ResponseEntity.status(HttpStatus.CREATED).body(bandStoryService.toResponse(story));
    }

    @DeleteMapping("/{storyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long bandId, @PathVariable Long storyId) {
        var user = CurrentUser.require();
        bandStoryService.delete(bandId, storyId, user.getId(), user.isPlatformAdmin());
    }
}
