package com.giglister.dto.event;

import com.giglister.dto.submission.SubmissionResponse;

/**
 * POST /api/events's response: either the event went straight live ({@code published},
 * {@code event} set) because the caller has direct create rights (platform admin, or EDIT+
 * on the location or a referenced band - see EventService.canCreateDirectly), or it was
 * routed into the review queue instead ({@code submission} set) for a platform admin to
 * approve or reject. Exactly one of {@code event}/{@code submission} is non-null.
 */
public record EventCreateResult(
        boolean published,
        EventResponse event,
        SubmissionResponse submission
) {
}
