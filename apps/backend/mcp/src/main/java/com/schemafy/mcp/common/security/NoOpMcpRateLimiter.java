package com.schemafy.mcp.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
class NoOpMcpRateLimiter implements McpRateLimiter {

  @Override
  public Mono<Boolean> tryAcquire(McpTokenClaims claims) {
    return Mono.just(true);
  }

}
