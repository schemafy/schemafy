package com.schemafy.mcp.common.security;

import java.util.Set;

public record McpTokenClaims(
    String tokenId,
    String userId,
    Set<String> scopes) {

  public boolean hasScope(String scope) {
    return scopes.contains(scope);
  }

}
