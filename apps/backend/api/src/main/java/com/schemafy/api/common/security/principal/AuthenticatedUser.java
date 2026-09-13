package com.schemafy.api.common.security.principal;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** 인증된 사용자 정보를 담는 UserDetails 구현체.
 * JWT 토큰에서 추출한 사용자 정보를 담으며, Spring Security의 표준 방식을 따른다.
 *
 * JWT 기반 인증에서는 password와 authority가 불필요하므로 각각 null과 빈 컬렉션을 반환한다. */
public record AuthenticatedUser(
    String userId,
    String userName) implements UserDetails {

  @Override
  public String getUsername() { return userId; }

  @Override
  public String getPassword() {
    // JWT 인증에서는 password 불필요
    return null;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptySet(); }

  @Override
  public boolean isAccountNonExpired() { return true; }

  @Override
  public boolean isAccountNonLocked() { return true; }

  @Override
  public boolean isCredentialsNonExpired() { return true; }

  @Override
  public boolean isEnabled() { return true; }

  public static AuthenticatedUser of(String userId) {
    return of(userId, "unknown");
  }

  public static AuthenticatedUser of(String userId, String userName) {
    return new AuthenticatedUser(userId, userName);
  }

}
