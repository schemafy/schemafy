package com.schemafy.core.project.domain;

public enum WorkspaceRole {

  ADMIN(2),
  MEMBER(1);

  private final int level;

  WorkspaceRole(int level) {
    this.level = level;
  }

  public static WorkspaceRole fromString(String value) {
    for (WorkspaceRole role : WorkspaceRole.values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown role: " + value);
  }

  public int getLevel() { return level; }

  public boolean isAdmin() { return this.level >= ADMIN.level; }

  public boolean isHigherOrEqualThan(WorkspaceRole other) {
    return this.level >= other.level;
  }

  public ProjectRole toProjectRole() {
    return isAdmin() ? ProjectRole.ADMIN : ProjectRole.VIEWER;
  }

}
