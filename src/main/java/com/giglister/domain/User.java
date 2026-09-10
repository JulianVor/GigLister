package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The single account type in GigLister. Differences between users are expressed
 * purely through {@link EntityPermission} grants and the {@code platformAdmin} flag,
 * never through a distinct account type.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    private String verificationToken;

    private Instant verificationTokenExpiresAt;

    private String homeCity;

    private Double homeLatitude;

    private Double homeLongitude;

    private Integer radiusKm;

    @Column(nullable = false)
    @Builder.Default
    private boolean platformAdmin = false;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
