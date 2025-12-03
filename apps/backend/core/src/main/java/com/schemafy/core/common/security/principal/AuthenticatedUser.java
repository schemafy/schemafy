package com.schemafy.core.common.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 인증된 사용자 정보를 담는 UserDetails 구현체.
 * JWT 토큰에서 추출한 사용자 정보를 담으며, Spring Security의 표준 방식을 따른다.
 *
 * JWT 기반 인증에서는 password가 불필요하므로 null을 반환한다.
 * 향후 워크스페이스/프로젝트 롤을 실제 데이터로 매핑할 예정이며,
 * 현재는 모든 프로젝트 롤을 부여해 hasAuthority 기반 체크를 통과시키는 임시 구현이다.
 */
public record AuthenticatedUser(
        String userId,
        Set<ProjectRole> roles // TODO: 사용자 역할을 저장/조회해 채워 넣는다.
) implements UserDetails {

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public String getPassword() {
        // JWT 인증에서는 password 불필요
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles == null ? Collections.emptySet()
                : roles.stream()
                        .map(ProjectRole::asAuthority)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

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

}
