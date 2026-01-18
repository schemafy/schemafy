package com.schemafy.core.project.repository.vo;

import lombok.Getter;

/** 프로젝트 역할. Level 기반으로 권한을 비교한다. */
@Getter
public enum ProjectRole {

  ADMIN(3, "admin"),
  EDITOR(2, "editor"),
  VIEWER(1, "viewer");

  private final int level;
  private final String value;

  ProjectRole(int level, String value) {
    this.level = level;
    this.value = value;
  }

  /** 현재 역할이 요구되는 역할 이상인지 확인
   *
   * @param required 요구되는 최소 역할
   * @return 현재 역할이 요구 역할 이상이면 true */
  public boolean isAtLeast(ProjectRole required) {
    return this.level >= required.level;
  }

  public boolean isAdmin() { return this.level >= ADMIN.level; }

  public boolean canEdit() {
    return this.level >= EDITOR.level;
  }

  /** Spring Security 권한 문자열 생성
   *
   * @return ROLE_ 접두사가 붙은 권한 문자열 (예: "ROLE_OWNER") */
  public String asAuthority() {
    return "ROLE_" + this.name();
  }

  public static ProjectRole fromString(String value) {
    for (ProjectRole role : ProjectRole.values()) {
      if (role.value.equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Invalid project role: " + value);
  }

  public boolean isHigherOrEqualThan(ProjectRole other) {
    return this.level >= other.level;
  }

}
