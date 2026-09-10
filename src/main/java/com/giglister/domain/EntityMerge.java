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
 * Audit record of a duplicate merge. The source entity's relationships are all
 * relinked to the target; the source's old name is kept as an alias so that old
 * links can redirect to the surviving entity.
 */
@Entity
@Table(name = "entity_merge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityMerge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column(nullable = false)
    private Long sourceEntityId;

    @Column(nullable = false)
    private Long targetEntityId;

    private String sourceNameAlias;

    @Column(nullable = false)
    private Long mergedBy;

    @Column(nullable = false)
    private Instant mergedAt;

    @PrePersist
    void onCreate() {
        if (mergedAt == null) {
            mergedAt = Instant.now();
        }
    }
}
