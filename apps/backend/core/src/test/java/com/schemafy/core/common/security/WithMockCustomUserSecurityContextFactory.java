package com.schemafy.core.common.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.project.repository.vo.ProjectRole;

public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(
            WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Set<ProjectRole> roles = Arrays.stream(customUser.roles())
                .map(ProjectRole::valueOf)
                .collect(Collectors.toSet());

        AuthenticatedUser principal = AuthenticatedUser
                .withRoles(customUser.userId(), roles);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        context.setAuthentication(authentication);
        return context;
    }

}
