package com.giglister.dto.common;

/**
 * Reference to an existing Band/Location by id, OR the minimal data to create
 * a new STUB for it inline (e.g. while creating an event). Exactly one of
 * {@code id} or {@code name} must be given.
 */
public record EntityRef(
        Long id,
        String name,
        String city,
        String address
) {
    public boolean isExisting() {
        return id != null;
    }
}
