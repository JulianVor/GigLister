package com.giglister.dto.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.giglister.domain.enums.SubmissionType;
import jakarta.validation.constraints.NotNull;

/**
 * {@code payload} must match BandCreateRequest/LocationCreateRequest/
 * EventCreateRequest's JSON shape, depending on {@code type} - it's stored
 * as-is and only parsed/validated into the concrete type once an admin
 * approves it.
 */
public record SubmissionCreateRequest(
        @NotNull SubmissionType type,
        @NotNull JsonNode payload,
        String imageUrl
) {
}
