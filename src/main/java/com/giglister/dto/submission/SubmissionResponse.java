package com.giglister.dto.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.domain.enums.SubmissionType;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        SubmissionType type,
        JsonNode payload,
        String imageUrl,
        SubmissionStatus status,
        Instant submittedAt,
        Long reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        Long resultEntityId
) {
}
