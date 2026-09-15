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

    /** Set when an admin creates this account with a temporary password (see
     * AdminService.createUser) - cleared the moment the user successfully changes their
     * password (see AuthService.changePassword), same login is still allowed either way.
     * A nullable `Boolean` (not primitive `boolean`) with a null-safe isMustChangePassword()
     * below - same shape as Event.bandImageDisplay, and for the same reason: with
     * `ddl-auto: update` and no migration tool, Hibernate adds this column via a plain
     * ALTER TABLE ADD COLUMN against a table that may already have live rows, and every
     * existing row gets NULL there since it has nothing to backfill from. A NOT NULL
     * column fails that ALTER TABLE outright; a NOT NULL primitive `boolean` field (even
     * with `@Column(nullable = true)` forcing the column itself to accept NULL) instead
     * lets the ALTER TABLE succeed but then throws the moment Hibernate tries to hydrate
     * one of those NULL rows into a primitive, since a primitive can never legally hold
     * null - both were tried and both broke login for every pre-existing account. Only an
     * actually-nullable Java type survives reading a NULL row back. */
    @Column(nullable = true)
    private Boolean mustChangePassword;

    public boolean isMustChangePassword() {
        return Boolean.TRUE.equals(mustChangePassword);
    }

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
