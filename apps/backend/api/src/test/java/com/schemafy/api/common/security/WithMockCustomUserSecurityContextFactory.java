package com.schemafy.api.common.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.schemafy.api.common.security.principal.AuthenticatedUser;

public class WithMockCustomUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomUser> {

  @Override
  public SecurityContext createSecurityContext(
      WithMockCustomUser customUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    AuthenticatedUser principal = AuthenticatedUser
        .of(customUser.userId());

    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());

    context.setAuthentication(authentication);
    return context;
  }

}
