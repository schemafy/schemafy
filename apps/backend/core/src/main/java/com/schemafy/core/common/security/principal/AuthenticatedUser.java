package com.schemafy.core.common.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 인증된 사용자 정보. 향후 워크스페이스/프로젝트 롤을 실제 데이터로 매핑할 예정이며,
 * 현재는 모든 프로젝트 롤을 부여해 hasAuthority 기반 체크를 통과시키는 임시 구현이다.
 */
public record AuthenticatedUser(
        String userId,
        Set<ProjectRole> roles // TODO: 사용자 역할을 저장/조회해 채워 넣는다.
) {

    public static AuthenticatedUser of(String userId) {
        return new AuthenticatedUser(userId, Collections.emptySet());
    }

    public static AuthenticatedUser withAllRoles(String userId) {
        return new AuthenticatedUser(userId,
                EnumSet.allOf(ProjectRole.class));
    }

    public static AuthenticatedUser withRoles(String userId,
            Set<ProjectRole> roles) {
        return new AuthenticatedUser(userId, roles);
    }

    public Collection<? extends GrantedAuthority> asAuthorities() {
        return roles == null ? Collections.emptyList()
                : roles.stream()
                        .map(ProjectRole::asAuthority)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toUnmodifiableSet());
    }

}
