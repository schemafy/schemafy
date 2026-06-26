package com.schemafy.mcp.common.security;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class McpTokenValidatorTest {

  private static final String REVOKED_TOKEN_ID = "revoked-token";

  McpSecurityProperties properties;
  McpTokenTestFactory tokenFactory;
  McpTokenValidator validator;
  TestMcpTokenRevocationStore revocationStore;

  @BeforeEach
  void setUp() {
    properties = new McpSecurityProperties();
    properties.getToken().setSecret("test-schemafy-mcp-secret-minimum-256-bit-key-value");
    properties.getToken().setIssuer("schemafy-mcp-test");
    properties.getToken().setAudience("schemafy-mcp-test");
    properties.getToken().setClockSkewSeconds(0);
    revocationStore = new TestMcpTokenRevocationStore();
    validator = new McpTokenValidator(properties, revocationStore);
    tokenFactory = new McpTokenTestFactory(properties);
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
    revocationStore.revoke(REVOKED_TOKEN_ID).block();

    expectFailure(validator.validate(tokenFactory.revokedToken(REVOKED_TOKEN_ID)),
        McpSecurityError.TOKEN_REVOKED);
  }

  private void expectFailure(Mono<McpTokenValidationResult> actual, McpSecurityError error) {
    StepVerifier.create(actual)
        .assertNext(result -> {
          assertThat(result.valid()).isFalse();
          assertThat(result.error()).isEqualTo(error);
        })
        .verifyComplete();
  }

  private static class TestMcpTokenRevocationStore
      implements McpTokenRevocationStore {

    private final Set<String> revokedTokenIds = ConcurrentHashMap.newKeySet();

    @Override
    public Mono<Boolean> isRevoked(String tokenId) {
      return Mono.just(tokenId != null && !tokenId.isBlank()
          && revokedTokenIds.contains(tokenId));
    }

    Mono<Void> revoke(String tokenId) {
      revokedTokenIds.add(tokenId);
      return Mono.empty();
    }

  }

}
