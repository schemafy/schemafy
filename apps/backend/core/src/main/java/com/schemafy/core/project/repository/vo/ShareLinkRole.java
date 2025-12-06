package com.schemafy.core.project.repository.vo;

public enum ShareLinkRole {

    VIEWER("viewer"),
    COMMENTER("commenter"),
    EDITOR("editor");

    private final String value;

    ShareLinkRole(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    public boolean canEdit() {
        return this == EDITOR;
    }

    public boolean canComment() {
        return this == EDITOR || this == COMMENTER;
    }

    public static ShareLinkRole fromString(String value) {
        for (ShareLinkRole role : ShareLinkRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid share link role: " + value);
    }

}
