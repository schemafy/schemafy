package com.schemafy.mcp.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
class NoOpMcpTokenRevocationStore implements McpTokenRevocationStore {

  @Override
  public Mono<Boolean> isRevoked(String tokenId) {
    return Mono.just(false);
  }

}
