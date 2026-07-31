package com.schemafy.mcp.common.security;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("MCP Redis disabled fallback")
class McpRedisDisabledFallbackIntegrationTest {

  @Autowired
  McpRateLimiter rateLimiter;

  @Autowired
  McpTokenRevocationCache revocationCache;

  @Test
  @DisplayName("Redis가 비활성화되어도 no-op fallback으로 보안 Bean을 구성한다")
  void configuresFallbackSecurityBeansWhenRedisIsDisabled() {
    McpTokenClaims claims = new McpTokenClaims("token-1", "user-1", Set.of("mcp"));

    StepVerifier.create(rateLimiter.tryAcquire(claims))
        .expectNext(true)
        .verifyComplete();
    StepVerifier.create(revocationCache.isRevoked(claims.tokenId()))
        .expectNext(false)
        .verifyComplete();
  }

}
