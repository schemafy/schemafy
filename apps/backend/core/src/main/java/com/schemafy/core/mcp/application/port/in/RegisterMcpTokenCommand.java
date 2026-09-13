package com.schemafy.core.mcp.application.port.in;

import java.time.Instant;

public record RegisterMcpTokenCommand(
    String tokenId,
    String userId,
    String scope,
    Instant issuedAt,
    Instant expiresAt) {
}
