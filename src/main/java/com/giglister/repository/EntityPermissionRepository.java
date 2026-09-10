package com.giglister.repository;

import com.giglister.domain.EntityPermission;
import com.giglister.domain.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntityPermissionRepository extends JpaRepository<EntityPermission, Long> {

    Optional<EntityPermission> findByUserIdAndEntityTypeAndEntityId(Long userId, EntityType entityType, Long entityId);

    List<EntityPermission> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    List<EntityPermission> findByUserId(Long userId);

    void deleteByUserIdAndEntityTypeAndEntityId(Long userId, EntityType entityType, Long entityId);

    List<EntityPermission> findByEntityTypeAndEntityIdIn(EntityType entityType, List<Long> entityIds);
}
