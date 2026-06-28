package com.schemafy.mcp.common.security;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisMcpRateLimiterTest {

  private static final String KEY = "mcp:rate-limit:user:user-1";

  @Mock
  ReactiveStringRedisTemplate redisTemplate;

  McpSecurityProperties properties;
  RedisMcpRateLimiter rateLimiter;

  @BeforeEach
  void setUp() {
    properties = new McpSecurityProperties();
    properties.getRateLimit().setRequests(2);
    properties.getRateLimit().setWindow(Duration.ofSeconds(30));
    rateLimiter = new RedisMcpRateLimiter(redisTemplate, properties);
  }

  @Test
  @DisplayName("Redis script로 카운터 증가와 TTL 설정을 원자적으로 수행한다")
  void incrementsCounterWithRedisScript() {
    when(redisTemplate.execute(
        McpSecurityRedisScripts.RATE_LIMIT_INCREMENT,
        List.of(KEY),
        List.of("30000")))
        .thenReturn(Flux.just(1L));

    StepVerifier.create(rateLimiter.tryAcquire(claims()))
        .expectNext(true)
        .verifyComplete();

    verify(redisTemplate).execute(
        McpSecurityRedisScripts.RATE_LIMIT_INCREMENT,
        List.of(KEY),
        List.of("30000"));
  }

  @Test
  @DisplayName("Redis 카운터가 허용치를 넘으면 요청을 거부한다")
  void rejectsWhenCounterExceedsLimit() {
    when(redisTemplate.execute(
        McpSecurityRedisScripts.RATE_LIMIT_INCREMENT,
        List.of(KEY),
        List.of("30000")))
        .thenReturn(Flux.just(3L));

    StepVerifier.create(rateLimiter.tryAcquire(claims()))
        .expectNext(false)
        .verifyComplete();

    verify(redisTemplate).execute(
        McpSecurityRedisScripts.RATE_LIMIT_INCREMENT,
        List.of(KEY),
        List.of("30000"));
  }

  private McpTokenClaims claims() {
    return new McpTokenClaims("token-1", "user-1", Set.of("mcp"));
  }

}
