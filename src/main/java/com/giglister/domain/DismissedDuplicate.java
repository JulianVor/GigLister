package com.giglister.domain;

import com.giglister.domain.enums.EntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An admin's "kein Duplikat" call on a pair AdminService.possibleDuplicates surfaced -
 * without this, the same pairwise scan would keep resurfacing it on every later dashboard
 * load forever, since it never persists anything on its own (see EntityMerge for the
 * opposite outcome, actually merging the two). lowerEntityId/higherEntityId are always
 * the pair's two ids in ascending order, not first/second from whichever pass first
 * surfaced them - the O(n^2) scan's iteration order isn't guaranteed stable across calls,
 * so a lookup has to be direction-independent to reliably suppress the same pair again.
 */
@Entity
@Table(name = "dismissed_duplicate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DismissedDuplicate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column(nullable = false)
    private Long lowerEntityId;

    @Column(nullable = false)
    private Long higherEntityId;

    @Column(nullable = false)
    private Long dismissedBy;

    @Column(nullable = false)
    private Instant dismissedAt;

    @PrePersist
    void onCreate() {
        if (dismissedAt == null) {
            dismissedAt = Instant.now();
        }
    }
}
