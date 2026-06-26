package com.schemafy.mcp.common.security;

import org.springframework.http.HttpStatus;

public enum McpSecurityError {

  TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "MCP_TOKEN_MISSING", "MCP Bearer token is required"),
  TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "MCP_TOKEN_MALFORMED", "MCP token is malformed"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "MCP_TOKEN_EXPIRED", "MCP token is expired"),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "MCP_TOKEN_INVALID", "MCP token is invalid"),
  TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "MCP_TOKEN_REVOKED", "MCP token is revoked"),
  INSUFFICIENT_SCOPE(HttpStatus.FORBIDDEN, "MCP_INSUFFICIENT_SCOPE", "MCP token scope is insufficient"),
  RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "MCP_RATE_LIMIT_EXCEEDED", "MCP rate limit exceeded");

  private final HttpStatus status;
  private final String code;
  private final String message;

  McpSecurityError(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }

  public String message() {
    return message;
  }

}
