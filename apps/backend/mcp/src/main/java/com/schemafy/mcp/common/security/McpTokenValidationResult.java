package com.schemafy.mcp.common.security;

public record McpTokenValidationResult(
    boolean valid,
    McpTokenClaims claims,
    McpSecurityError error) {

  public static McpTokenValidationResult success(McpTokenClaims claims) {
    return new McpTokenValidationResult(true, claims, null);
  }

  public static McpTokenValidationResult failure(McpSecurityError error) {
    return new McpTokenValidationResult(false, null, error);
  }

}
