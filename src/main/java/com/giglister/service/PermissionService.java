package com.giglister.service;

import com.giglister.domain.EntityPermission;
import com.giglister.domain.User;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.dto.PermissionResponse;
import com.giglister.exception.ForbiddenException;
import com.giglister.exception.NotFoundException;
import com.giglister.repository.EntityPermissionRepository;
import com.giglister.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Central place for checking and granting the two object-specific permission
 * levels (EDIT, MANAGE) a user can hold on a Band or Location. Platform admins
 * always pass every check, without needing an explicit grant.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final EntityPermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public Optional<PermissionLevel> levelOf(Long userId, EntityType type, Long entityId) {
        if (userId == null) {
            return Optional.empty();
        }
        return permissionRepository.findByUserIdAndEntityTypeAndEntityId(userId, type, entityId)
                .map(EntityPermission::getPermission);
    }

    public boolean has(User user, EntityType type, Long entityId, PermissionLevel required) {
        if (user == null) {
            return false;
        }
        if (user.isPlatformAdmin()) {
            return true;
        }
        return levelOf(user.getId(), type, entityId)
                .map(level -> level.atLeast(required))
                .orElse(false);
    }

    public boolean has(Long userId, boolean platformAdmin, EntityType type, Long entityId, PermissionLevel required) {
        if (platformAdmin) {
            return true;
        }
        return levelOf(userId, type, entityId)
                .map(level -> level.atLeast(required))
                .orElse(false);
    }

    public void require(Long userId, boolean platformAdmin, EntityType type, Long entityId, PermissionLevel required) {
        if (!has(userId, platformAdmin, type, entityId, required)) {
            throw new ForbiddenException("Requires " + required + " permission on " + type + " " + entityId);
        }
    }

    public boolean isUnclaimed(EntityType type, Long entityId) {
        return permissionRepository.findByEntityTypeAndEntityId(type, entityId).stream()
                .noneMatch(p -> p.getPermission() == PermissionLevel.MANAGE);
    }

    @Transactional
    public EntityPermission grant(EntityType type, Long entityId, Long userId, PermissionLevel level, Long grantedBy) {
        EntityPermission existing = permissionRepository
                .findByUserIdAndEntityTypeAndEntityId(userId, type, entityId)
                .orElse(null);
        if (existing != null) {
            existing.setPermission(level);
            return permissionRepository.save(existing);
        }
        return permissionRepository.save(EntityPermission.builder()
                .userId(userId)
                .entityType(type)
                .entityId(entityId)
                .permission(level)
                .grantedBy(grantedBy)
                .build());
    }

    @Transactional
    public void revoke(EntityType type, Long entityId, Long userId) {
        permissionRepository.deleteByUserIdAndEntityTypeAndEntityId(userId, type, entityId);
    }

    public List<PermissionResponse> listHolders(EntityType type, Long entityId) {
        return permissionRepository.findByEntityTypeAndEntityId(type, entityId).stream()
                .map(p -> {
                    User u = userRepository.findById(p.getUserId())
                            .orElseThrow(() -> new NotFoundException("User not found"));
                    return new PermissionResponse(u.getId(), u.getEmail(), u.getUsername(), p.getPermission());
                })
                .toList();
    }

    @Transactional
    public void relinkOnMerge(EntityType type, Long sourceId, Long targetId) {
        List<EntityPermission> sourcePermissions = permissionRepository.findByEntityTypeAndEntityId(type, sourceId);
        for (EntityPermission perm : sourcePermissions) {
            Optional<EntityPermission> targetPerm = permissionRepository
                    .findByUserIdAndEntityTypeAndEntityId(perm.getUserId(), type, targetId);
            if (targetPerm.isPresent()) {
                PermissionLevel merged = targetPerm.get().getPermission().atLeast(perm.getPermission())
                        ? targetPerm.get().getPermission() : perm.getPermission();
                targetPerm.get().setPermission(merged);
                permissionRepository.save(targetPerm.get());
            } else {
                perm.setEntityId(targetId);
                permissionRepository.save(perm);
            }
        }
        permissionRepository.findByEntityTypeAndEntityId(type, sourceId)
                .forEach(p -> permissionRepository.deleteByUserIdAndEntityTypeAndEntityId(p.getUserId(), type, sourceId));
    }
}
