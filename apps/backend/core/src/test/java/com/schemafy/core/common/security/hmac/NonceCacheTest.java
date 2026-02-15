package com.schemafy.core.common.security.hmac;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonceCacheTest {

  @Mock
  ReactiveStringRedisTemplate redisTemplate;

  @Mock
  ReactiveValueOperations<String, String> valueOps;

  NonceCache nonceCache;

  @BeforeEach
  void setUp() {
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    HmacProperties props = new HmacProperties();
    props.setTimestampToleranceSeconds(30);
    nonceCache = new NonceCache(redisTemplate, props);
  }

  @Test
  @DisplayName("새로운 nonce는 중복이 아니다")
  void newNonceIsNotDuplicate() {
    given(valueOps.setIfAbsent(anyString(), eq("1"),
        eq(Duration.ofSeconds(60))))
        .willReturn(Mono.just(true));

    StepVerifier.create(nonceCache.isDuplicate("nonce-1"))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  @DisplayName("동일한 nonce를 재사용하면 중복으로 판단한다")
  void duplicateNonceIsDetected() {
    given(valueOps.setIfAbsent(anyString(), eq("1"),
        eq(Duration.ofSeconds(60))))
        .willReturn(Mono.just(false));

    StepVerifier.create(nonceCache.isDuplicate("nonce-1"))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("null nonce는 중복으로 판단한다")
  void nullNonceIsDuplicate() {
    StepVerifier.create(nonceCache.isDuplicate(null))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("빈 nonce는 중복으로 판단한다")
  void blankNonceIsDuplicate() {
    StepVerifier.create(nonceCache.isDuplicate(""))
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(nonceCache.isDuplicate("   "))
        .expectNext(true)
        .verifyComplete();
  }

}
