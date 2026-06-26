package com.schemafy.mcp.common.security;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMcpRateLimiter implements McpRateLimiter {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final McpSecurityProperties properties;

  public RedisMcpRateLimiter(
      ReactiveStringRedisTemplate redisTemplate,
      McpSecurityProperties properties) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
  }

  @Override
  public Mono<Boolean> tryAcquire(McpTokenClaims claims) {
    McpSecurityProperties.RateLimit rateLimit = properties.getRateLimit();
    if (!rateLimit.isEnabled()) {
      return Mono.just(true);
    }

    String key = rateLimit.getKeyPrefix() + claims.userId();
    String windowMillis = Long.toString(rateLimit.getWindow().toMillis());
    return redisTemplate.execute(
        McpSecurityRedisScripts.RATE_LIMIT_INCREMENT,
        List.of(key),
        List.of(windowMillis))
        .next()
        .map(count -> count <= rateLimit.getRequests());
  }

}
