package com.schemafy.core.project.repository.vo;

public enum ProjectRole {
    OWNER("owner"),
    ADMIN("admin"),
    EDITOR("editor"),
    VIEWER("viewer");

    private final String value;

    ProjectRole(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    public boolean isOwner() { return this == OWNER; }

    public boolean isAdmin() { return this == OWNER || this == ADMIN; }

    public boolean canEdit() {
        return this == OWNER || this == ADMIN || this == EDITOR;
    }

    public static ProjectRole fromString(String value) {
        for (ProjectRole role : ProjectRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid project role: " + value);
    }
}
