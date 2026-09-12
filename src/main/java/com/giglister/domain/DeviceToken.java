package com.giglister.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single device's Firebase Cloud Messaging registration, for push notifications about
 * new concerts near a user's saved home location (see PushNotificationService). Keyed by
 * the token itself, not (userId, token) - a token belongs to one app install, which can
 * only ever be logged into one account at a time, so a fresh login re-registering the
 * same token (e.g. after switching accounts on one phone) reassigns it instead of leaving
 * a duplicate row still pointing at the previous owner.
 */
@Entity
@Table(name = "device_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
