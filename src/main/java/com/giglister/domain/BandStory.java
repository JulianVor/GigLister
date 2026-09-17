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

    // How the band positioned/scaled/rotated imageUrl within the 9:16 story frame (see
    // BandStoryComposer) - the image itself is never modified, these numbers alone are enough
    // to reproduce the exact same view everywhere the story is shown (see CroppedStoryImage
    // on the frontend). Center-based (not top-left-based) so rotating around it is simple CSS.
    // Nullable so a story predating this feature (none exist yet, but the columns are nullable
    // defensively) just falls back to plain object-contain instead of breaking.
    private Double imgWidthPct;
    private Double imgHeightPct;
    private Double imgCenterXPct;
    private Double imgCenterYPct;
    private Double imgRotationDeg;
    // Fills whatever the image doesn't cover (it's never forced to cover the whole frame
    // anymore - the band can zoom out until the entire photo is visible, or rotate it,
    // leaving gaps) - the average color sampled from the photo itself, not a flat black.
    private String imgBackgroundColor;

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
