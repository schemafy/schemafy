package com.schemafy.mcp.common.security;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;

import reactor.core.publisher.Mono;

public class McpAuthenticationWebFilter implements WebFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final McpTokenValidator tokenValidator;
  private final McpRateLimiter rateLimiter;
  private final McpSecurityErrorWriter errorWriter;
  private final McpSecurityAuditLogger auditLogger;

  public McpAuthenticationWebFilter(
      McpTokenValidator tokenValidator,
      McpRateLimiter rateLimiter,
      McpSecurityErrorWriter errorWriter,
      McpSecurityAuditLogger auditLogger) {
    this.tokenValidator = tokenValidator;
    this.rateLimiter = rateLimiter;
    this.errorWriter = errorWriter;
    this.auditLogger = auditLogger;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!requiresMcpSecurity(exchange.getRequest())) {
      return chain.filter(exchange);
    }

    String token = extractBearerToken(exchange.getRequest());
    return tokenValidator.validate(token)
        .flatMap(result -> {
          if (!result.valid()) {
            auditLogger.authenticationFailed(exchange, result.error());
            return errorWriter.write(exchange, result.error());
          }
          McpTokenClaims claims = result.claims();
          return rateLimiter.tryAcquire(claims)
              .flatMap(allowed -> {
                if (!allowed) {
                  auditLogger.rateLimited(exchange, claims);
                  return errorWriter.write(exchange,
                      McpSecurityError.RATE_LIMIT_EXCEEDED);
                }
                UsernamePasswordAuthenticationToken authentication = createAuthentication(claims);
                auditLogger.authenticationSucceeded(exchange, claims);
                return chain.filter(exchange)
                    .contextWrite(context -> ErdOperationContexts.withActorUserId(claims.userId())
                        .apply(context
                            .putAll(ReactiveSecurityContextHolder
                                .withAuthentication(authentication)
                                .readOnly())
                            .putAll(ProjectAccessRequesterContext
                                .withRequesterId(claims.userId())
                                .readOnly())));
              });
        });
  }

  private boolean requiresMcpSecurity(ServerHttpRequest request) {
    String path = request.getPath().pathWithinApplication().value();
    return "/mcp".equals(path) || path.startsWith("/mcp/");
  }

  private String extractBearerToken(ServerHttpRequest request) {
    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(authorization)
        && authorization.regionMatches(true, 0, BEARER_PREFIX, 0,
            BEARER_PREFIX.length())) {
      return authorization.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  private UsernamePasswordAuthenticationToken createAuthentication(McpTokenClaims claims) {
    McpAuthenticatedPrincipal principal = new McpAuthenticatedPrincipal(
        claims.userId(),
        claims.scopes(),
        claims.tokenId());
    List<SimpleGrantedAuthority> authorities = claims.scopes().stream()
        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
        .toList();
    return new UsernamePasswordAuthenticationToken(principal, null, authorities);
  }

}
