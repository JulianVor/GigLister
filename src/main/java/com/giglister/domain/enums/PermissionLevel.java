package com.giglister.domain.enums;

/**
 * EDIT: may maintain the entity's content.
 * MANAGE: EDIT plus the ability to manage who else has permissions.
 */
public enum PermissionLevel {
    EDIT,
    MANAGE;

    public boolean atLeast(PermissionLevel other) {
        return this.ordinal() >= other.ordinal();
    }
}
