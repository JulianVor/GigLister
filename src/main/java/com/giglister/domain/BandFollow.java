package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "band_follow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "band_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(nullable = false)
    private Instant followedAt;

    @PrePersist
    void onCreate() {
        if (followedAt == null) {
            followedAt = Instant.now();
        }
    }
}
