package com.schemafy.core.workspace.repository.vo;

import lombok.Getter;

@Getter
public enum WorkspaceRole {
    ADMIN("admin"),
    MEMBER("member");

    private final String value;

    WorkspaceRole(String value) {
        this.value = value;
    }

    public static WorkspaceRole fromValue(String value) {
        for (WorkspaceRole role : WorkspaceRole.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }

    public boolean isAdmin() { return this == ADMIN; }
}
