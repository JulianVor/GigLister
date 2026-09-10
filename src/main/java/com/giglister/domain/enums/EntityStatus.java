package com.giglister.domain.enums;

/**
 * Lifecycle status shared by Band and Location.
 * STUB: only reference data, no public profile page yet.
 * DRAFT: more data present, but not publicly navigable yet.
 * PUBLISHED: publicly visible with its own profile page.
 * ARCHIVED: no longer active, historical relationships are kept.
 */
public enum EntityStatus {
    STUB,
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
