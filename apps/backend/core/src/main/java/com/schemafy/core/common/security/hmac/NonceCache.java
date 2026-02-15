package com.schemafy.core.common.security.hmac;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "hmac", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NonceCache {

  private static final String KEY_PREFIX = "hmac:nonce:";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final Duration ttl;

  public NonceCache(ReactiveStringRedisTemplate redisTemplate,
      HmacProperties hmacProperties) {
    this.redisTemplate = redisTemplate;
    this.ttl = Duration.ofSeconds(
        (long) hmacProperties.getTimestampToleranceSeconds() * 2);
  }

  // 원자적으로 중복 + 삽입 연산하기 위해서
  public Mono<Boolean> isDuplicate(String nonce) {
    if (nonce == null || nonce.isBlank()) {
      return Mono.just(true);
    }

    return redisTemplate.opsForValue()
        .setIfAbsent(KEY_PREFIX + nonce, "1", ttl)
        .map(created -> !created);
  }

}
