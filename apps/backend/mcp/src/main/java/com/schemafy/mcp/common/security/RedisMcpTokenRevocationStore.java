package com.schemafy.mcp.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMcpTokenRevocationStore implements McpTokenRevocationStore {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final McpSecurityProperties properties;

  public RedisMcpTokenRevocationStore(
      ReactiveStringRedisTemplate redisTemplate,
      McpSecurityProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  @Override
  public Mono<Boolean> isRevoked(String tokenId) {
    if (!StringUtils.hasText(tokenId)) {
      return Mono.just(false);
    }
    return redisTemplate.hasKey(revocationKey(tokenId));
  }

  private String revocationKey(String tokenId) {
    return properties.getToken().getRevocationKeyPrefix() + tokenId;
  }

}
