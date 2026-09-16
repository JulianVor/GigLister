package com.giglister.dto.common;

import java.time.LocalTime;

/**
 * Reference to an existing Band/Location by id, OR the minimal data to create
 * a new STUB for it inline (e.g. while creating an event). Exactly one of
 * {@code id} or {@code name} must be given.
 */
public record EntityRef(
        Long id,
        String name,
        String city,
        String address,
        String postalCode,
        /** Only meaningful when this ref is one of an Event's `bands` - this band's own
         * start time within the show, distinct from the Event's own overall startTime.
         * Null means it shares the event's overall time. Ignored for `location`. */
        LocalTime startTime
) {
    public boolean isExisting() {
        return id != null;
    }
}
