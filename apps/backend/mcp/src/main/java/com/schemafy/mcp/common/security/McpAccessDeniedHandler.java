package com.schemafy.mcp.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class McpAccessDeniedHandler implements ServerAccessDeniedHandler {

  private final McpSecurityErrorWriter errorWriter;

  public McpAccessDeniedHandler(McpSecurityErrorWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
    return errorWriter.write(exchange, McpSecurityError.INSUFFICIENT_SCOPE);
  }

}
