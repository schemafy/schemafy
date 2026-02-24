package com.schemafy.core.project.repository.vo;

/** 프로젝트 역할. Level 기반으로 권한을 비교한다. */
public enum ProjectRole {

  ADMIN(3),
  EDITOR(2),
  VIEWER(1);

  private final int level;

  ProjectRole(int level) {
    this.level = level;
  }

  public int getLevel() { return level; }

  public boolean isAdmin() { return this.level >= ADMIN.level; }

  public boolean canEdit() {
    return this.level >= EDITOR.level;
  }

  /** Spring Security 권한 문자열 생성
   *
   * @return ROLE_ 접두사가 붙은 권한 문자열 (예: "ROLE_ADMIN") */
  public String asAuthority() {
    return "ROLE_" + this.name();
  }

  public static ProjectRole fromString(String value) {
    for (ProjectRole role : ProjectRole.values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Invalid project role: " + value);
  }

  public boolean isHigherOrEqualThan(ProjectRole other) {
    return this.level >= other.level;
  }

}
