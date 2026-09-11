package com.giglister.web;

import com.giglister.dto.submission.SubmissionCreateRequest;
import com.giglister.dto.submission.SubmissionResponse;
import com.giglister.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The only endpoint the external GPT-skill integration can reach (enforced
 * in SecurityConfig via ROLE_GPT_SKILL) - everything it submits lands in the
 * review queue. See AdminController for the approve/reject side.
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionResponse> submit(@Valid @RequestBody SubmissionCreateRequest request) {
        var submission = submissionService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.toResponse(submission));
    }
}
