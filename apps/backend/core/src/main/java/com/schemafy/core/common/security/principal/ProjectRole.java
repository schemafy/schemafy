package com.schemafy.core.common.security.principal;

/**
 * 프로젝트 단위 역할. ERD 엔드포인트 접근 제어 시 최소 역할 비교용으로 사용한다.
 */
public enum ProjectRole {

    OWNER(5),
    ADMIN(4),
    EDITOR(3),
    COMMENTER(2),
    VIEWER(1);

    private final int level;

    ProjectRole(int level) {
        this.level = level;
    }

    public boolean isAtLeast(ProjectRole required) {
        return this.level >= required.level;
    }

    public String asAuthority() {
        return "ROLE_" + this.name();
    }

}
