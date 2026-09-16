package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One band a user has "gemerkt" within a specific festival concert - distinct from
 * SavedEvent (the whole concert). Only makes sense for a band that has its own start time
 * within that concert (see BandLineupEntry): with no per-band time there's nothing to pick
 * out from the rest of the line-up. Saving an act also saves its whole Event (see
 * UserService.saveAct) so the concert itself still shows up everywhere a saved event does -
 * this table only ever narrows further within an already-saved concert. */
@Entity
@Table(
        name = "saved_act",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id", "band_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedAct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(nullable = false)
    private Instant savedAt;

    @PrePersist
    void onCreate() {
        if (savedAt == null) {
            savedAt = Instant.now();
        }
    }
}
