package com.giglister.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giglister.domain.Submission;
import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.dto.band.BandCreateRequest;
import com.giglister.dto.event.EventCreateRequest;
import com.giglister.dto.location.LocationCreateRequest;
import com.giglister.dto.submission.SubmissionCreateRequest;
import com.giglister.dto.submission.SubmissionResponse;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.ConflictException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.SubmissionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The review queue behind the GPT-skill integration: everything it submits
 * lands here as PENDING and only ever becomes a real Band/Location/Event
 * once a platform admin approves it (see SubmissionController vs.
 * AdminController). Rejecting leaves no trace beyond the Submission row
 * itself - nothing was ever created.
 */
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final BandService bandService;
    private final LocationService locationService;
    private final EventService eventService;
    private final ImageFetchService imageFetchService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Transactional
    public Submission submit(SubmissionCreateRequest request) {
        Submission submission = Submission.builder()
                .type(request.type())
                .payload(request.payload().toString())
                .imageUrl(blankToNull(request.imageUrl()))
                .status(SubmissionStatus.PENDING)
                .build();
        return submissionRepository.save(submission);
    }

    public List<Submission> list(SubmissionStatus status) {
        return status == null
                ? submissionRepository.findAllByOrderBySubmittedAtDesc()
                : submissionRepository.findByStatusOrderBySubmittedAtDesc(status);
    }

    public Submission getOrThrow(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Submission " + id + " not found"));
    }

    /**
     * Deserializes+validates the stored payload into the real create request, downloads the image (if
     * any) only now, then runs the exact same create path a human using the normal forms would.
     */
    @Transactional
    public Submission approve(Long id, Long adminUserId) {
        Submission submission = getOrThrow(id);
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new ConflictException("Submission already decided");
        }

        String localImageUrl = submission.getImageUrl() != null
                ? imageFetchService.fetchAndStore(submission.getImageUrl())
                : null;

        Long resultId = switch (submission.getType()) {
            case BAND -> approveBand(submission, adminUserId, localImageUrl);
            case LOCATION -> approveLocation(submission, adminUserId, localImageUrl);
            case EVENT -> approveEvent(submission, adminUserId, localImageUrl);
        };

        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setResultEntityId(resultId);
        submission.setReviewedBy(adminUserId);
        submission.setReviewedAt(Instant.now());
        return submissionRepository.save(submission);
    }

    @Transactional
    public Submission reject(Long id, Long adminUserId, String reason) {
        Submission submission = getOrThrow(id);
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new ConflictException("Submission already decided");
        }
        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setReviewedBy(adminUserId);
        submission.setReviewedAt(Instant.now());
        submission.setRejectionReason(blankToNull(reason));
        return submissionRepository.save(submission);
    }

    private Long approveBand(Submission submission, Long adminUserId, String imageUrl) {
        var band = bandService.create(parsePayload(submission, BandCreateRequest.class), adminUserId);
        if (imageUrl != null) {
            band = bandService.setTitleImage(band.getId(), imageUrl);
        }
        return band.getId();
    }

    private Long approveLocation(Submission submission, Long adminUserId, String imageUrl) {
        var location = locationService.create(parsePayload(submission, LocationCreateRequest.class), adminUserId);
        if (imageUrl != null) {
            location = locationService.setTitleImage(location.getId(), imageUrl);
        }
        return location.getId();
    }

    private Long approveEvent(Submission submission, Long adminUserId, String imageUrl) {
        EventCreateRequest request = parsePayload(submission, EventCreateRequest.class);
        if (imageUrl != null) {
            request = new EventCreateRequest(request.title(), request.date(), request.startTime(),
                    request.location(), request.bands(), request.description(), request.ticketUrl(), imageUrl);
        }
        return eventService.create(request, adminUserId).getId();
    }

    private <T> T parsePayload(Submission submission, Class<T> type) {
        T value;
        try {
            value = objectMapper.readValue(submission.getPayload(), type);
        } catch (Exception e) {
            throw new BadRequestException("Der gespeicherte Vorschlag ist ungültig formatiert: " + e.getMessage());
        }
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BadRequestException("Vorschlag unvollständig oder ungültig: " + message);
        }
        return value;
    }

    private JsonNode payloadAsJson(Submission submission) {
        try {
            return objectMapper.readTree(submission.getPayload());
        } catch (Exception e) {
            return objectMapper.getNodeFactory().objectNode();
        }
    }

    public SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(), submission.getType(), payloadAsJson(submission), submission.getImageUrl(),
                submission.getStatus(), submission.getSubmittedAt(), submission.getReviewedBy(),
                submission.getReviewedAt(), submission.getRejectionReason(), submission.getResultEntityId()
        );
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
