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
import com.giglister.dto.submission.SubmissionUpdateRequest;
import com.giglister.exception.BadRequestException;
import com.giglister.exception.ConflictException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.SubmissionRepository;
import com.giglister.repository.UserRepository;
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
 * The review queue behind the GPT-skill integration and, for events, behind
 * EventController.create() when the submitting user has no direct create
 * rights: everything lands here as PENDING and only ever becomes a real
 * Band/Location/Event once a platform admin approves it (see
 * SubmissionController vs. AdminController). Rejecting leaves no trace
 * beyond the Submission row itself - nothing was ever created.
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
    private final UserRepository userRepository;

    /** The GPT-skill integration has no giglister account of its own - its submissions are
     * never attributed to a particular user (see submittedBy on approve()). */
    @Transactional
    public Submission submit(SubmissionCreateRequest request) {
        return submit(request, null);
    }

    /** submittedBy: set when a logged-in user submitted this themselves (see
     * EventController.create), so they're attributed as the result entity's creator on
     * approval instead of the approving admin - null for the GPT-skill integration. */
    @Transactional
    public Submission submit(SubmissionCreateRequest request, Long submittedBy) {
        if (request.targetEntityId() != null) {
            switch (request.type()) {
                case BAND -> bandService.getOrThrow(request.targetEntityId());
                case LOCATION -> locationService.getOrThrow(request.targetEntityId());
                case EVENT -> throw new BadRequestException("targetEntityId wird nur für BAND/LOCATION unterstützt");
            }
        }
        Submission submission = Submission.builder()
                .type(request.type())
                .payload(request.payload().toString())
                .imageUrl(blankToNull(request.imageUrl()))
                .targetEntityId(request.targetEntityId())
                .submittedBy(submittedBy)
                .status(SubmissionStatus.PENDING)
                .build();
        return submissionRepository.save(submission);
    }

    public List<Submission> list(SubmissionStatus status) {
        return status == null
                ? submissionRepository.findAllByOrderBySubmittedAtDesc()
                : submissionRepository.findByStatusOrderBySubmittedAtDesc(status);
    }

    /** "Meine Vorschläge": everything this user has submitted themselves (see submittedBy),
     * newest first - regardless of status, so they can see a rejection reason too. */
    public List<Submission> mine(Long userId) {
        return submissionRepository.findBySubmittedByOrderBySubmittedAtDesc(userId);
    }

    /** Lets an admin correct the proposed data before approving - e.g. a wrong address or a typo'd name. */
    @Transactional
    public Submission update(Long id, SubmissionUpdateRequest request) {
        Submission submission = getOrThrow(id);
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new ConflictException("Submission already decided");
        }
        submission.setPayload(request.payload().toString());
        submission.setImageUrl(blankToNull(request.imageUrl()));
        return submissionRepository.save(submission);
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
            case BAND -> submission.getTargetEntityId() != null
                    ? approveBandEnrichment(submission, localImageUrl)
                    : approveBand(submission, adminUserId, localImageUrl);
            case LOCATION -> submission.getTargetEntityId() != null
                    ? approveLocationEnrichment(submission, localImageUrl)
                    : approveLocation(submission, adminUserId, localImageUrl);
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

    /** submittedBy null means the GPT-skill integration proposed this, not a person who
     * filled out the form themselves - see createFromAutomatedProposal for why that starts
     * it at STUB/PUBLISHED instead of the usual DRAFT a direct create() lands on. */
    private Long approveBand(Submission submission, Long adminUserId, String imageUrl) {
        BandCreateRequest request = parsePayload(submission, BandCreateRequest.class);
        Long createdBy = resolveCreatedBy(submission, adminUserId);
        var band = submission.getSubmittedBy() == null
                ? bandService.createFromAutomatedProposal(request, createdBy)
                : bandService.create(request, createdBy);
        if (imageUrl != null) {
            band = bandService.setTitleImage(band.getId(), imageUrl);
        }
        return band.getId();
    }

    private Long approveLocation(Submission submission, Long adminUserId, String imageUrl) {
        LocationCreateRequest request = parsePayload(submission, LocationCreateRequest.class);
        Long createdBy = resolveCreatedBy(submission, adminUserId);
        var location = submission.getSubmittedBy() == null
                ? locationService.createFromAutomatedProposal(request, createdBy)
                : locationService.create(request, createdBy);
        if (imageUrl != null) {
            location = locationService.setTitleImage(location.getId(), imageUrl);
        }
        return location.getId();
    }

    /** Whoever submitted this themselves becomes the result entity's creator (so they keep
     * normal edit/delete rights over what they proposed) - falls back to the approving admin
     * for the GPT-skill integration, which has no submittedBy of its own. */
    private Long resolveCreatedBy(Submission submission, Long adminUserId) {
        return submission.getSubmittedBy() != null ? submission.getSubmittedBy() : adminUserId;
    }

    private Long approveBandEnrichment(Submission submission, String imageUrl) {
        var band = bandService.applyEnrichment(submission.getTargetEntityId(),
                parsePayload(submission, BandCreateRequest.class), imageUrl);
        return band.getId();
    }

    private Long approveLocationEnrichment(Submission submission, String imageUrl) {
        var location = locationService.applyEnrichment(submission.getTargetEntityId(),
                parsePayload(submission, LocationCreateRequest.class), imageUrl);
        return location.getId();
    }

    private Long approveEvent(Submission submission, Long adminUserId, String imageUrl) {
        EventCreateRequest request = parsePayload(submission, EventCreateRequest.class);
        if (imageUrl != null) {
            request = new EventCreateRequest(request.title(), request.date(), request.startTime(),
                    request.location(), request.bands(), request.description(), request.ticketUrl(), imageUrl,
                    request.bandImageDisplay(), request.eventSeriesId());
        }
        return eventService.create(request, resolveCreatedBy(submission, adminUserId)).getId();
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
        String submittedByUsername = submission.getSubmittedBy() != null
                ? userRepository.findById(submission.getSubmittedBy()).map(u -> u.getUsername()).orElse(null)
                : null;
        return new SubmissionResponse(
                submission.getId(), submission.getType(), payloadAsJson(submission), submission.getImageUrl(),
                submission.getStatus(), submission.getSubmittedAt(), submission.getReviewedBy(),
                submission.getReviewedAt(), submission.getRejectionReason(), submission.getResultEntityId(),
                submission.getTargetEntityId(), submission.getSubmittedBy(), submittedByUsername
        );
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
