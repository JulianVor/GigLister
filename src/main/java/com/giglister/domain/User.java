package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    private String resetToken;

    private Instant resetTokenExpiresAt;

    private String homeCity;

    private Double homeLatitude;

    private Double homeLongitude;

    private Integer radiusKm;

    /** Base genres (see GenreTaxonomy) the user explicitly picked in their settings - feeds
     * the "Das könnte dich interessieren" recommendations on Entdecken, in addition to
     * genres implicitly derived from followed bands. Eager for the same reason as
     * Band.genres - DTO mapping happens outside the transaction. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferred_genre", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "genre")
    @Builder.Default
    private List<String> preferredGenres = new ArrayList<>();

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
