package com.schemafy.mcp.common.security;

import java.security.Principal;
import java.util.Set;

public record McpAuthenticatedPrincipal(
    String userId,
    Set<String> scopes,
    String tokenId) implements Principal {

  @Override
  public String getName() { return userId; }

}
