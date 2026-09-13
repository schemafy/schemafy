package com.schemafy.api.mcp.service;

import java.time.Instant;

public record McpTokenIssueResult(
    String token,
    String tokenId,
    String scope,
    Instant issuedAt,
    Instant expiresAt,
    long expiresInSeconds) {
}
