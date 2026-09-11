package com.giglister.domain;

import com.giglister.domain.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "band")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Band {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String city;

    private String country;

    @Column(length = 2000)
    private String shortDescription;

    private String website;

    private String logoUrl;

    private String titleImageUrl;

    // Eager for the same reason as Event.bandIds - DTO mapping happens outside the transaction.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "band_genre", joinColumns = @JoinColumn(name = "band_id"))
    @Column(name = "genre")
    @Builder.Default
    private List<String> genres = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EntityStatus status = EntityStatus.STUB;

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
