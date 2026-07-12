package com.schemafy.mcp.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.mcp.application.port.in.GetMcpTokenQuery;
import com.schemafy.core.mcp.application.port.in.GetMcpTokenUseCase;
import com.schemafy.core.mcp.domain.McpToken;

import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class McpTokenValidatorTest {

  private static final String REVOKED_TOKEN_ID = "revoked-token";
  private static final Instant NOW = Instant.parse("2026-06-27T00:00:00Z");

  McpSecurityProperties properties;
  McpTokenTestFactory tokenFactory;
  McpTokenValidator validator;
  TestMcpTokenRevocationCache revocationCache;
  TestGetMcpTokenUseCase getMcpTokenUseCase;

  @BeforeEach
  void setUp() {
    properties = new McpSecurityProperties();
    properties.getToken().setSecret("test-schemafy-mcp-secret-minimum-256-bit-key-value");
    properties.getToken().setIssuer("schemafy-mcp-test");
    properties.getToken().setAudience("schemafy-mcp-test");
    revocationCache = new TestMcpTokenRevocationCache();
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    SecretKey secretKey = Keys.hmacShaKeyFor(
        properties.getToken().getSecret().getBytes(StandardCharsets.UTF_8));
    getMcpTokenUseCase = new TestGetMcpTokenUseCase(properties, clock);
    validator = new McpTokenValidator(properties, revocationCache,
        getMcpTokenUseCase, clock, secretKey);
    tokenFactory = new McpTokenTestFactory(properties, clock);
  }

  @Test
  @DisplayName("유효한 MCP 토큰을 검증한다")
  void validatesToken() {
    StepVerifier.create(validator.validate(tokenFactory.validToken()))
        .assertNext(result -> {
          assertThat(result.valid()).isTrue();
          assertThat(result.claims().userId()).isEqualTo("user-1");
          assertThat(result.claims().hasScope("mcp")).isTrue();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("누락된 토큰을 거부한다")
  void rejectsMissingToken() {
    expectFailure(Mono.defer(() -> validator.validate(null)), McpSecurityError.TOKEN_MISSING);
  }

  @Test
  @DisplayName("형식이 잘못된 토큰을 거부한다")
  void rejectsMalformedToken() {
    expectFailure(validator.validate("not-a-jwt"), McpSecurityError.TOKEN_MALFORMED);
  }

  @Test
  @DisplayName("만료된 토큰을 거부한다")
  void rejectsExpiredToken() {
    expectFailure(validator.validate(tokenFactory.expiredToken()), McpSecurityError.TOKEN_EXPIRED);
  }

  @Test
  @DisplayName("issuer가 다른 토큰을 거부한다")
  void rejectsWrongIssuer() {
    expectFailure(validator.validate(tokenFactory.wrongIssuerToken()), McpSecurityError.TOKEN_INVALID);
  }

  @Test
  @DisplayName("audience가 다른 토큰을 거부한다")
  void rejectsWrongAudience() {
    expectFailure(validator.validate(tokenFactory.wrongAudienceToken()), McpSecurityError.TOKEN_INVALID);
  }

  @Test
  @DisplayName("MCP 토큰 타입이 아닌 토큰을 거부한다")
  void rejectsWrongTokenType() {
    expectFailure(validator.validate(tokenFactory.wrongTokenTypeToken()), McpSecurityError.TOKEN_INVALID);
  }

  @Test
  @DisplayName("필수 MCP scope가 없는 토큰을 거부한다")
  void rejectsMissingScope() {
    expectFailure(validator.validate(tokenFactory.tokenWithoutRequiredScope()),
        McpSecurityError.INSUFFICIENT_SCOPE);
  }

  @Test
  @DisplayName("폐기된 토큰을 거부한다")
  void rejectsRevokedToken() {
    revocationCache.revoke(REVOKED_TOKEN_ID).block();

    expectFailure(validator.validate(tokenFactory.revokedToken(REVOKED_TOKEN_ID)),
        McpSecurityError.TOKEN_REVOKED);
  }

  @Test
  @DisplayName("Redis revocation 조회가 실패하면 fail-closed로 거부한다")
  void rejectsWhenRevocationCheckUnavailable() {
    revocationCache.fail();

    expectFailure(validator.validate(tokenFactory.validToken()),
        McpSecurityError.REVOCATION_CHECK_UNAVAILABLE);
  }

  @Test
  @DisplayName("DB token registry에 등록되지 않은 토큰을 거부한다")
  void rejectsUnregisteredToken() {
    getMcpTokenUseCase.clear();

    expectFailure(validator.validate(tokenFactory.validToken()),
        McpSecurityError.TOKEN_INVALID);
  }

  @Test
  @DisplayName("DB token registry에서 폐기된 토큰을 거부한다")
  void rejectsTokenRevokedInRegistry() {
    getMcpTokenUseCase.revoke("token-1");

    expectFailure(validator.validate(tokenFactory.validToken()),
        McpSecurityError.TOKEN_REVOKED);
  }

  @Test
  @DisplayName("DB token registry 조회가 실패하면 fail-closed로 거부한다")
  void rejectsWhenTokenRegistryUnavailable() {
    getMcpTokenUseCase.fail();

    expectFailure(validator.validate(tokenFactory.validToken()),
        McpSecurityError.TOKEN_REGISTRY_UNAVAILABLE);
  }

  private void expectFailure(Mono<McpTokenValidationResult> actual, McpSecurityError error) {
    StepVerifier.create(actual)
        .assertNext(result -> {
          assertThat(result.valid()).isFalse();
          assertThat(result.error()).isEqualTo(error);
        })
        .verifyComplete();
  }

  private static class TestMcpTokenRevocationCache
      implements McpTokenRevocationCache {

    private final Set<String> revokedTokenIds = ConcurrentHashMap.newKeySet();
    private boolean fail;

    @Override
    public Mono<Boolean> isRevoked(String tokenId) {
      if (fail) {
        return Mono.error(new IllegalStateException("Redis unavailable"));
      }
      return Mono.just(tokenId != null && !tokenId.isBlank()
          && revokedTokenIds.contains(tokenId));
    }

    Mono<Void> revoke(String tokenId) {
      revokedTokenIds.add(tokenId);
      return Mono.empty();
    }

    void fail() {
      this.fail = true;
    }

  }

  private static class TestGetMcpTokenUseCase implements GetMcpTokenUseCase {

    private final Map<String, McpToken> tokens = new ConcurrentHashMap<>();
    private final Clock clock;
    private boolean fail;

    TestGetMcpTokenUseCase(McpSecurityProperties properties, Clock clock) {
      this.clock = clock;
      McpToken token = McpToken.issue(
          "token-1",
          "user-1",
          properties.getToken().getRequiredScope(),
          clock.instant().minusSeconds(60),
          clock.instant().plusSeconds(3600));
      tokens.put(token.getId(), token);
    }

    @Override
    public Mono<McpToken> getMcpToken(GetMcpTokenQuery query) {
      if (fail) {
        return Mono.error(new IllegalStateException("Token registry unavailable"));
      }
      return Mono.justOrEmpty(tokens.get(query.tokenId()));
    }

    void clear() {
      tokens.clear();
    }

    void revoke(String tokenId) {
      tokens.get(tokenId).revoke(clock.instant());
    }

    void fail() {
      this.fail = true;
    }

  }

}
