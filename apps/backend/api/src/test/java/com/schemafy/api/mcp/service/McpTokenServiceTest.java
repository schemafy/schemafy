package com.schemafy.api.mcp.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.mcp.exception.McpTokenErrorCode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenUseCase;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenUseCase;
import com.schemafy.core.mcp.domain.McpToken;
import com.schemafy.core.mcp.domain.McpTokenClaimSupport;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class McpTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-27T00:00:00Z");

  McpTokenProperties properties;
  FakeMcpTokenRevocationCache revocationCache;
  FakeMcpTokenUseCase mcpTokenUseCase;
  McpTokenService tokenService;

  @BeforeEach
  void setUp() {
    properties = new McpTokenProperties();
    properties.setSecret("test-schemafy-mcp-secret-minimum-256-bit-key-value");
    properties.setIssuer("schemafy-mcp-test");
    properties.setAudience("schemafy-mcp-test");
    properties.setExpiresIn(Duration.ofMinutes(15));
    revocationCache = new FakeMcpTokenRevocationCache();
    mcpTokenUseCase = new FakeMcpTokenUseCase();
    tokenService = new McpTokenService(
        properties,
        revocationCache,
        mcpTokenUseCase,
        mcpTokenUseCase,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("MCP 서버가 검증할 수 있는 claim으로 토큰을 발급한다")
  void issuesMcpTokenClaims() {
    McpTokenIssueResult result = tokenService.issue("user-1").block();

    Claims claims = parseClaims(result.token());
    assertThat(result.token()).isNotBlank();
    assertThat(result.tokenId()).isEqualTo(claims.getId());
    assertThat(result.scope()).isEqualTo("mcp");
    assertThat(result.issuedAt()).isEqualTo(NOW);
    assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    assertThat(result.expiresInSeconds()).isEqualTo(900);
    assertThat(claims.getSubject()).isEqualTo("user-1");
    assertThat(claims.getIssuer()).isEqualTo("schemafy-mcp-test");
    assertThat(claims.getAudience()).contains("schemafy-mcp-test");
    assertThat(claims.get(McpTokenClaimSupport.TYPE, String.class)).isEqualTo("MCP");
    assertThat(claims.get(McpTokenClaimSupport.SCOPE, String.class)).isEqualTo("mcp");
    assertThat(mcpTokenUseCase.savedTokens())
        .singleElement()
        .satisfies(savedToken -> {
          assertThat(savedToken.getId()).isEqualTo(result.tokenId());
          assertThat(savedToken.getUserId()).isEqualTo("user-1");
          assertThat(savedToken.getScope()).isEqualTo("mcp");
          assertThat(savedToken.getIssuedAt()).isEqualTo(NOW);
          assertThat(savedToken.getExpiresAt())
              .isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        });
  }

  @Test
  @DisplayName("사용자 자신의 MCP 토큰을 revoke하면 DB와 Redis cache에 폐기 상태를 저장한다")
  void revokesOwnMcpToken() {
    McpTokenIssueResult result = tokenService.issue("user-1").block();

    StepVerifier.create(tokenService.revoke("user-1", result.token()))
        .verifyComplete();

    assertThat(mcpTokenUseCase.revokedTokens())
        .singleElement()
        .satisfies(revoked -> {
          assertThat(revoked.tokenId()).isEqualTo(result.tokenId());
          assertThat(revoked.userId()).isEqualTo("user-1");
          assertThat(revoked.revokedAt()).isEqualTo(NOW);
        });
    assertThat(revocationCache.revokedTokens())
        .singleElement()
        .satisfies(revoked -> {
          assertThat(revoked.tokenId()).isEqualTo(result.tokenId());
          assertThat(revoked.ttl()).isEqualTo(Duration.ofMinutes(15));
        });
  }

  @Test
  @DisplayName("다른 사용자의 MCP 토큰은 revoke할 수 없다")
  void rejectsRevokingAnotherUsersToken() {
    McpTokenIssueResult result = tokenService.issue("user-1").block();

    StepVerifier.create(tokenService.revoke("user-2", result.token()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(McpTokenErrorCode.OWNER_MISMATCH);
        })
        .verify();
    assertThat(mcpTokenUseCase.revokedTokens()).isEmpty();
    assertThat(revocationCache.revokedTokens()).isEmpty();
  }

  @Test
  @DisplayName("DB token registry에 발급 상태를 저장할 수 없으면 발급을 거부한다")
  void rejectsIssueWhenTokenRegistryUnavailable() {
    mcpTokenUseCase.failSave();

    StepVerifier.create(tokenService.issue("user-1"))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(McpTokenErrorCode.REGISTRY_UNAVAILABLE);
        })
        .verify();
  }

  @Test
  @DisplayName("DB token registry에 폐기 상태를 저장할 수 없으면 revoke를 거부한다")
  void rejectsRevokeWhenTokenRegistryUnavailable() {
    McpTokenIssueResult result = tokenService.issue("user-1").block();
    mcpTokenUseCase.failRevoke();

    StepVerifier.create(tokenService.revoke("user-1", result.token()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(McpTokenErrorCode.REGISTRY_UNAVAILABLE);
        })
        .verify();
  }

  @Test
  @DisplayName("Redis revocation cache가 없어도 DB registry에 revoke를 저장한다")
  void revokesWhenRevocationCacheUnavailable() {
    McpTokenService serviceWithoutCache = new McpTokenService(
        properties,
        null,
        mcpTokenUseCase,
        mcpTokenUseCase,
        Clock.fixed(NOW, ZoneOffset.UTC));
    McpTokenIssueResult result = serviceWithoutCache.issue("user-1").block();

    StepVerifier.create(serviceWithoutCache.revoke("user-1", result.token()))
        .verifyComplete();
    assertThat(mcpTokenUseCase.revokedTokens())
        .singleElement()
        .satisfies(revoked -> assertThat(revoked.tokenId())
            .isEqualTo(result.tokenId()));
  }

  private Claims parseClaims(String token) {
    SecretKey secretKey = Keys.hmacShaKeyFor(
        properties.getSecret().getBytes(StandardCharsets.UTF_8));
    return Jwts.parser()
        .verifyWith(secretKey)
        .clock(() -> Date.from(NOW))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private static class FakeMcpTokenRevocationCache
      implements McpTokenRevocationCache {

    private final List<RevokedToken> revokedTokens = new ArrayList<>();

    @Override
    public Mono<Void> cacheRevocation(String tokenId, Duration ttl) {
      revokedTokens.add(new RevokedToken(tokenId, ttl));
      return Mono.empty();
    }

    List<RevokedToken> revokedTokens() {
      return revokedTokens;
    }

  }

  private static class FakeMcpTokenUseCase implements
      RegisterMcpTokenUseCase,
      RevokeMcpTokenUseCase {

    private final List<McpToken> savedTokens = new ArrayList<>();
    private final List<RevokedTokenRecord> revokedTokens = new ArrayList<>();
    private boolean failSave;
    private boolean failRevoke;

    @Override
    public Mono<McpToken> registerMcpToken(RegisterMcpTokenCommand command) {
      if (failSave) {
        return Mono.error(new IllegalStateException("Token registry unavailable"));
      }
      McpToken token = McpToken.issue(
          command.tokenId(),
          command.userId(),
          command.scope(),
          command.issuedAt(),
          command.expiresAt());
      savedTokens.add(token);
      return Mono.just(token);
    }

    @Override
    public Mono<Boolean> revokeMcpToken(RevokeMcpTokenCommand command) {
      if (failRevoke) {
        return Mono.error(new IllegalStateException("Token registry unavailable"));
      }
      revokedTokens.add(new RevokedTokenRecord(
          command.tokenId(),
          command.userId(),
          command.revokedAt()));
      return Mono.just(true);
    }

    List<McpToken> savedTokens() {
      return savedTokens;
    }

    List<RevokedTokenRecord> revokedTokens() {
      return revokedTokens;
    }

    void failSave() {
      this.failSave = true;
    }

    void failRevoke() {
      this.failRevoke = true;
    }

  }

  private record RevokedToken(String tokenId, Duration ttl) {
  }

  private record RevokedTokenRecord(
      String tokenId,
      String userId,
      Instant revokedAt) {
  }

}
