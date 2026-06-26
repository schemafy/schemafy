package com.schemafy.mcp.common.security;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisMcpTokenRevocationStoreTest {

  @Mock
  ReactiveStringRedisTemplate redisTemplate;

  RedisMcpTokenRevocationStore revocationStore;

  @BeforeEach
  void setUp() {
    revocationStore = new RedisMcpTokenRevocationStore(
        redisTemplate,
        new McpSecurityProperties());
  }

  @Test
  @DisplayName("폐기된 토큰 여부를 Redis key 존재 여부로 확인한다")
  void checksRevokedTokenWithRedisKey() {
    when(redisTemplate.hasKey("mcp:token:revoked:token-1"))
        .thenReturn(Mono.just(true));

    StepVerifier.create(revocationStore.isRevoked("token-1"))
        .expectNext(true)
        .verifyComplete();

    verify(redisTemplate).hasKey("mcp:token:revoked:token-1");
  }

  @Test
  @DisplayName("토큰 ID가 비어 있으면 Redis를 조회하지 않는다")
  void skipsRedisLookupForBlankTokenId() {
    StepVerifier.create(revocationStore.isRevoked(" "))
        .expectNext(false)
        .verifyComplete();

    verifyNoInteractions(redisTemplate);
  }

}
