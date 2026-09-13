package com.schemafy.mcp.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class McpSecurityConfig {

  @Bean
  public SecurityWebFilterChain mcpSecurityWebFilterChain(
      ServerHttpSecurity http,
      McpTokenValidator tokenValidator,
      McpRateLimiter rateLimiter,
      McpSecurityErrorWriter errorWriter,
      McpSecurityAuditLogger auditLogger,
      McpAuthenticationEntryPoint authenticationEntryPoint,
      McpAccessDeniedHandler accessDeniedHandler,
      McpSecurityProperties properties) {
    String requiredAuthority = "SCOPE_" + properties.getToken().getRequiredScope();
    McpAuthenticationWebFilter authenticationWebFilter = new McpAuthenticationWebFilter(
        tokenValidator,
        rateLimiter,
        errorWriter,
        auditLogger);
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .securityContextRepository(
            NoOpServerSecurityContextRepository.getInstance())
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .requestCache(requestCache -> requestCache
            .requestCache(NoOpServerRequestCache.getInstance()))
        .exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .addFilterAt(authenticationWebFilter,
            SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
            .pathMatchers("/mcp", "/mcp/**").hasAuthority(requiredAuthority)
            .anyExchange().denyAll())
        .build();
  }

}
