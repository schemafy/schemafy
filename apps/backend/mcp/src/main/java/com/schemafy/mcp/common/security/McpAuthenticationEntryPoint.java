package com.schemafy.mcp.common.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class McpAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

  private final McpSecurityErrorWriter errorWriter;

  public McpAuthenticationEntryPoint(McpSecurityErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    return errorWriter.write(exchange, McpSecurityError.TOKEN_MISSING);
  }

}
