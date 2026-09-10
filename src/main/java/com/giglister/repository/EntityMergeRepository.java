package com.giglister.repository;

import com.giglister.domain.EntityMerge;
import com.giglister.domain.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntityMergeRepository extends JpaRepository<EntityMerge, Long> {
    Optional<EntityMerge> findByEntityTypeAndSourceEntityId(EntityType entityType, Long sourceEntityId);
}
