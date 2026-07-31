package com.schemafy.core.mcp.application.port.in;

import java.time.Instant;

public record RevokeMcpTokenCommand(
    String tokenId,
    String userId,
    Instant revokedAt) {
}
