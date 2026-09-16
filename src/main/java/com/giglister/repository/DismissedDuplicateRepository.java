package com.giglister.repository;

import com.giglister.domain.DismissedDuplicate;
import com.giglister.domain.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DismissedDuplicateRepository extends JpaRepository<DismissedDuplicate, Long> {
    boolean existsByEntityTypeAndLowerEntityIdAndHigherEntityId(EntityType entityType, Long lowerEntityId, Long higherEntityId);

    List<DismissedDuplicate> findByEntityType(EntityType entityType);
}
