package com.giglister.dto.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.giglister.domain.enums.SubmissionType;
import jakarta.validation.constraints.NotNull;

/**
 * {@code payload} must match BandCreateRequest/LocationCreateRequest/
 * EventCreateRequest's JSON shape, depending on {@code type} - it's stored
 * as-is and only parsed/validated into the concrete type once an admin
 * approves it.
 *
 * {@code targetEntityId}: leave unset to propose a brand-new Band/Location/Event
 * (the original behavior). Set it (type BAND or LOCATION only) to instead propose
 * enriching an existing STUB/DRAFT one found via GET /api/gpt/bands/incomplete or
 * /api/gpt/locations/incomplete - the payload only needs the fields being filled
 * in/corrected, everything else can be left out and the existing value stays.
 */
public record SubmissionCreateRequest(
        @NotNull SubmissionType type,
        @NotNull JsonNode payload,
        String imageUrl,
        Long targetEntityId
) {
}
