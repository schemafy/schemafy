package com.schemafy.api.mcp.service;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.api.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnRedisEnabled
@RequiredArgsConstructor
public class RedisMcpTokenRevocationCache implements McpTokenRevocationCache {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final McpTokenProperties properties;

  @Override
  public Mono<Void> cacheRevocation(String tokenId, Duration ttl) {
    return redisTemplate.opsForValue()
        .set(properties.getRevocationKeyPrefix() + tokenId, "1", ttl)
        .then();
  }

}
