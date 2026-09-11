package com.giglister.dto.submission;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/** Lets an admin correct a PENDING submission's data before approving it - type can't
 * change here (reject and ask for a resubmission if the skill got the type itself wrong). */
public record SubmissionUpdateRequest(
        @NotNull JsonNode payload,
        String imageUrl
) {
}
