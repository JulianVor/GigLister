package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A curated group of {@link Event}s that belong together under one name - e.g. a
 * festival or a themed night spread across several venues, like "SüdKultur MusicNight"
 * (several separate Events, each still its own concert with its own Location/line-up,
 * that all happen to be part of the same night). Unlike Band/Location there's no
 * STUB/DRAFT/PUBLISHED lifecycle here: a series is only ever created deliberately (by
 * name, never inferred from a bare reference), so it's live and linkable the moment it
 * exists, the same way an Event itself is (see EventService.create).
 */
@Entity
@Table(name = "event_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 4000)
    private String description;

    private String titleImageUrl;

    /** A shared ticket link for the whole night/festival, separate from any individual
     * Event's own ticketUrl (a single combined ticket vs. per-show tickets are both
     * realistic - this doesn't replace an Event's own link, just adds one at the series
     * level for whichever a given series actually uses). */
    private String ticketUrl;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
