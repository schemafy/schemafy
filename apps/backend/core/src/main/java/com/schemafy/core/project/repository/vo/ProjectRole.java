package com.schemafy.core.project.repository.vo;

public enum ProjectRole {

    OWNER("owner"),
    ADMIN("admin"),
    EDITOR("editor"),
    COMMENTER("commenter"),
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

    public boolean canComment() {
        return this == OWNER || this == ADMIN || this == EDITOR
                || this == COMMENTER;
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
