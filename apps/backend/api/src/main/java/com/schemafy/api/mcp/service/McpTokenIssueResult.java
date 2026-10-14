package com.schemafy.api.mcp.service;

import java.time.Instant;
import java.util.Set;

import com.schemafy.core.mcp.domain.McpTokenClaimSupport;

public record McpTokenIssueResult(
    String token,
    String tokenId,
    Set<String> scopes,
    Instant issuedAt,
    Instant expiresAt,
    long expiresInSeconds) {

  public String scope() {
    return McpTokenClaimSupport.canonicalScopeValue(scopes);
  }

}
