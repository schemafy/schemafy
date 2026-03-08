package com.schemafy.core.project.repository.vo;

public enum WorkspaceRole {

  ADMIN,
  MEMBER;

  public static WorkspaceRole fromString(String value) {
    for (WorkspaceRole role : WorkspaceRole.values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown role: " + value);
  }

  public boolean isAdmin() { return this == ADMIN; }

  public ProjectRole toProjectRole() {
    return this == ADMIN ? ProjectRole.ADMIN : ProjectRole.VIEWER;
  }

}
