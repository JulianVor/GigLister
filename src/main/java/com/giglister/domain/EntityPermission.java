package com.giglister.domain;

import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Object-specific right a user holds on a Band or Location. A user can hold
 * any number of these across different entities - permissions are never tied
 * to a special account type.
 */
@Entity
@Table(
        name = "entity_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionLevel permission;

    private Long grantedBy;

    @Column(nullable = false)
    private Instant grantedAt;

    @PrePersist
    void onCreate() {
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }
}
