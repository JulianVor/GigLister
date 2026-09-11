package com.giglister.domain;

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
    @Column(name = "band_id", nullable = false)
    @OrderColumn(name = "position")
    @Builder.Default
    private List<Long> bandIds = new ArrayList<>();

    @Column(length = 4000)
    private String description;

    private String ticketUrl;

    private String titleImageUrl;

    /** Whether the event's own listings/collage show each band's logo or its title
     * (promo) photo - see BandImageDisplay. Defaults to LOGO. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BandImageDisplay bandImageDisplay = BandImageDisplay.LOGO;

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
