package com.schemafy.core.mcp.adapter.out.persistence;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.mcp.domain.McpToken;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ McpTokenPersistenceAdapter.class, R2dbcTestConfiguration.class })
@DisplayName("McpTokenPersistenceAdapter")
class McpTokenPersistenceAdapterTest {

  @Autowired
  McpTokenPersistenceAdapter sut;

  @Autowired
  McpTokenRepository mcpTokenRepository;

  @BeforeEach
  void setUp() {
    mcpTokenRepository.deleteAll().block();
  }

  @Test
  @DisplayName("registerMcpToken/findMcpTokenById: MCP 토큰 발급 상태를 저장하고 조회한다")
  void registerMcpTokenAndFindMcpTokenById() {
    Instant issuedAt = Instant.parse("2026-06-27T00:00:00Z");
    Instant expiresAt = issuedAt.plusSeconds(900);
    String userId = UlidGenerator.generate();
    McpToken token = McpToken.issue(
        UlidGenerator.generate(),
        userId,
        "mcp",
        issuedAt,
        expiresAt);

    StepVerifier.create(sut.registerMcpToken(token))
        .assertNext(saved -> {
          assertThat(saved.getId()).isEqualTo(token.getId());
          assertThat(saved.getUserId()).isEqualTo(userId);
          assertThat(saved.getScope()).isEqualTo("mcp");
          assertThat(saved.getIssuedAt()).isEqualTo(issuedAt);
          assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
          assertThat(saved.getRevokedAt()).isNull();
          assertThat(saved.getCreatedAt()).isNotNull();
        })
        .verifyComplete();

    StepVerifier.create(sut.findMcpTokenById(token.getId()))
        .assertNext(found -> assertThat(found.getUserId()).isEqualTo(userId))
        .verifyComplete();
  }

  @Test
  @DisplayName("revoke: 사용자 자신의 MCP 토큰에 revoked_at을 기록한다")
  void revokeOwnToken() {
    Instant issuedAt = Instant.parse("2026-06-27T00:00:00Z");
    Instant revokedAt = issuedAt.plusSeconds(60);
    String userId = UlidGenerator.generate();
    McpToken token = sut.registerMcpToken(McpToken.issue(
        UlidGenerator.generate(),
        userId,
        "mcp",
        issuedAt,
        issuedAt.plusSeconds(900))).block();

    StepVerifier.create(sut.revokeMcpToken(token.getId(), userId, revokedAt))
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(sut.findMcpTokenById(token.getId()))
        .assertNext(found -> assertThat(found.getRevokedAt()).isEqualTo(revokedAt))
        .verifyComplete();
  }

  @Test
  @DisplayName("revoke: 다른 사용자의 MCP 토큰이면 갱신하지 않는다")
  void revokeAnotherUsersTokenReturnsFalse() {
    Instant issuedAt = Instant.parse("2026-06-27T00:00:00Z");
    String userId = UlidGenerator.generate();
    McpToken token = sut.registerMcpToken(McpToken.issue(
        UlidGenerator.generate(),
        userId,
        "mcp",
        issuedAt,
        issuedAt.plusSeconds(900))).block();

    StepVerifier.create(sut.revokeMcpToken(
        token.getId(),
        "user-2",
        issuedAt.plusSeconds(60)))
        .expectNext(false)
        .verifyComplete();

    StepVerifier.create(sut.findMcpTokenById(token.getId()))
        .assertNext(found -> assertThat(found.getRevokedAt()).isNull())
        .verifyComplete();
  }

  @Test
  @DisplayName("revoke: 이미 폐기된 MCP 토큰이면 revoked_at을 덮어쓰지 않고 성공 처리한다")
  void revokeAlreadyRevokedTokenReturnsTrueWithoutOverwritingRevokedAt() {
    Instant issuedAt = Instant.parse("2026-06-27T00:00:00Z");
    Instant firstRevokedAt = issuedAt.plusSeconds(60);
    Instant secondRevokedAt = issuedAt.plusSeconds(120);
    String userId = UlidGenerator.generate();
    McpToken token = sut.registerMcpToken(McpToken.issue(
        UlidGenerator.generate(),
        userId,
        "mcp",
        issuedAt,
        issuedAt.plusSeconds(900))).block();

    StepVerifier.create(sut.revokeMcpToken(token.getId(), userId, firstRevokedAt))
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(sut.revokeMcpToken(token.getId(), userId, secondRevokedAt))
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(sut.findMcpTokenById(token.getId()))
        .assertNext(found -> assertThat(found.getRevokedAt()).isEqualTo(firstRevokedAt))
        .verifyComplete();
  }

}
