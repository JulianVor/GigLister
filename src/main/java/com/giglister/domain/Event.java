package com.giglister.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.giglister.domain.enums.BandImageDisplay;
import com.giglister.domain.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The core entity of the platform: a concert that happens at a {@link Location}
 * and features one or more {@link Band}s. Relationships are always stored via
 * stable numeric IDs, never via names, so that entities can be renamed or merged
 * without breaking any relationship.
 */
@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional - most concerts are identified by their line-up, not a title. */
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime startTime;

    @Column(nullable = false)
    private Long locationId;

    // Eager: every DTO mapping needs the line-up, and that mapping happens outside the
    // request's transaction (open-in-view is off), so a lazy collection would throw
    // LazyInitializationException.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_band", joinColumns = @JoinColumn(name = "event_id"))
    @OrderColumn(name = "position")
    @Builder.Default
    private List<BandLineupEntry> bandLineup = new ArrayList<>();

    /** Convenience over bandLineup for callers that only need "which bands, in order" -
     * permission checks, genre filtering, notifications, admin listings, search... -
     * without caring about each band's own optional start time. @JsonIgnore: without it,
     * this getter-shaped derived method leaks in as a plain "bandIds" JSON property
     * wherever an Event is serialized directly (see DataTransferService) - harmless on
     * its own, except Jackson deserializing that same JSON back finds no setBandIds() but
     * does find this getter, and falls back to calling it and mutating the list it
     * returns in place; Stream#toList()'s result is immutable, so that throws
     * UnsupportedOperationException on every re-import. */
    @JsonIgnore
    public List<Long> getBandIds() {
        return bandLineup.stream().map(BandLineupEntry::getBandId).toList();
    }

    /** Swaps every lineup entry for oldBandId to newBandId (used when merging two Bands
     * into one) - keeps that entry's position and its own start time, since the same slot
     * is now just filled by a different (merged) band identity. Also dedupes by band id
     * afterward, keeping the first entry, in case the target band was already elsewhere
     * in the lineup. */
    public void replaceBandInLineup(Long oldBandId, Long newBandId) {
        for (BandLineupEntry entry : bandLineup) {
            if (entry.getBandId().equals(oldBandId)) {
                entry.setBandId(newBandId);
            }
        }
        LinkedHashMap<Long, BandLineupEntry> deduped = new LinkedHashMap<>();
        for (BandLineupEntry entry : bandLineup) {
            deduped.putIfAbsent(entry.getBandId(), entry);
        }
        bandLineup = new ArrayList<>(deduped.values());
    }

    @Column(length = 4000)
    private String description;

    private String ticketUrl;

    private String titleImageUrl;

    /** Optional - which EventSeries (festival/themed night, e.g. "SüdKultur MusicNight")
     * this concert is part of, if any. Nullable at the DB level for the same reason as
     * bandImageDisplay below: ddl-auto: update adds this column via a plain ALTER TABLE
     * against a table that may already have rows. */
    private Long eventSeriesId;

    /** Whether the event's own listings/collage show each band's logo or its title
     * (promo) photo - see BandImageDisplay. Defaults to PHOTO.
     *
     * Deliberately nullable at the DB level (no `nullable = false`) even though it's
     * conceptually required: with `ddl-auto: update` and no migration tool, Hibernate
     * adds this column via a plain ALTER TABLE ADD COLUMN against a table that may
     * already have rows - a NOT NULL column there fails immediately (existing rows
     * have nothing to backfill it with). Nullable avoids that; getBandImageDisplay()
     * below is the single place that treats a null (only possible on a pre-existing
     * row that hasn't been saved since) as PHOTO, so nothing else needs to care. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BandImageDisplay bandImageDisplay = BandImageDisplay.PHOTO;

    public BandImageDisplay getBandImageDisplay() {
        return bandImageDisplay != null ? bandImageDisplay : BandImageDisplay.PHOTO;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

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
