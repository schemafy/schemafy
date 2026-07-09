package com.schemafy.mcp.common.security;

import reactor.core.publisher.Mono;

public interface McpRateLimiter {

  Mono<Boolean> tryAcquire(McpTokenClaims claims);

}
