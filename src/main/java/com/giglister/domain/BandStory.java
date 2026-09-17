package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "band_story")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(length = 280)
    private String text;

    // How the band positioned/zoomed imageUrl within the 9:16 story frame (see
    // BandStoryComposer) - the image itself is never modified, these four numbers alone are
    // enough to reproduce the exact same crop everywhere the story is shown (see
    // CroppedStoryImage on the frontend). Nullable so a story predating this feature (none
    // exist yet, but the columns are nullable defensively) just falls back to plain
    // object-contain instead of breaking.
    private Double imgWidthPct;
    private Double imgHeightPct;
    private Double imgOffsetLeftPct;
    private Double imgOffsetTopPct;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plus(24, ChronoUnit.HOURS);
        }
    }
}
