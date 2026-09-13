package com.schemafy.mcp.common.security;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class McpSecurityAuditLogger {

  private static final Logger log = LoggerFactory.getLogger(McpSecurityAuditLogger.class);

  public void authenticationSucceeded(ServerWebExchange exchange, McpTokenClaims claims) {
    log.debug("mcp_auth_success path={} userId={} scopes={}",
        path(exchange), claims.userId(), claims.scopes());
  }

  public void authenticationFailed(ServerWebExchange exchange, McpSecurityError error) {
    log.warn("mcp_auth_failed path={} remoteAddress={} error={}",
        path(exchange), remoteAddress(exchange), error.code());
  }

  public void rateLimited(ServerWebExchange exchange, McpTokenClaims claims) {
    log.warn("mcp_rate_limited path={} userId={}",
        path(exchange), claims.userId());
  }

  private String path(ServerWebExchange exchange) {
    return exchange.getRequest().getPath().pathWithinApplication().value();
  }

  private String remoteAddress(ServerWebExchange exchange) {
    var remoteAddress = exchange.getRequest().getRemoteAddress();
    return remoteAddress != null ? remoteAddress.toString() : "unknown";
  }

}
