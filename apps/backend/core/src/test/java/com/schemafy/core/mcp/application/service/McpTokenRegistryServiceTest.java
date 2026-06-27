package com.schemafy.core.mcp.application.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.mcp.application.port.in.GetMcpTokenQuery;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenCommand;
import com.schemafy.core.mcp.application.port.out.FindMcpTokenByIdPort;
import com.schemafy.core.mcp.application.port.out.RegisterMcpTokenPort;
import com.schemafy.core.mcp.application.port.out.RevokeMcpTokenPort;
import com.schemafy.core.mcp.domain.McpToken;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpTokenRegistryService")
class McpTokenRegistryServiceTest {

  @Mock
  RegisterMcpTokenPort registerMcpTokenPort;

  @Mock
  RevokeMcpTokenPort revokeMcpTokenPort;

  @Mock
  FindMcpTokenByIdPort findMcpTokenByIdPort;

  @InjectMocks
  McpTokenRegistryService sut;

  @Test
  @DisplayName("registerMcpToken: MCP 토큰 도메인을 생성해 저장한다")
  void registerMcpToken() {
    Instant issuedAt = Instant.parse("2026-06-27T00:00:00Z");
    Instant expiresAt = issuedAt.plusSeconds(900);
    given(registerMcpTokenPort.registerMcpToken(any(McpToken.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.registerMcpToken(new RegisterMcpTokenCommand(
        "token-1",
        "user-1",
        "mcp",
        issuedAt,
        expiresAt)))
        .assertNext(token -> {
          assertThat(token.getId()).isEqualTo("token-1");
          assertThat(token.getUserId()).isEqualTo("user-1");
          assertThat(token.getScope()).isEqualTo("mcp");
          assertThat(token.getIssuedAt()).isEqualTo(issuedAt);
          assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("revokeMcpToken: revoke 결과를 반환한다")
  void revokeMcpToken() {
    Instant revokedAt = Instant.parse("2026-06-27T00:01:00Z");
    given(revokeMcpTokenPort.revokeMcpToken("token-1", "user-1", revokedAt))
        .willReturn(Mono.just(true));

    StepVerifier.create(sut.revokeMcpToken(new RevokeMcpTokenCommand(
        "token-1",
        "user-1",
        revokedAt)))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("getMcpToken: tokenId로 MCP 토큰을 조회한다")
  void getMcpToken() {
    McpToken token = McpToken.issue(
        "token-1",
        "user-1",
        "mcp",
        Instant.parse("2026-06-27T00:00:00Z"),
        Instant.parse("2026-06-27T00:15:00Z"));
    given(findMcpTokenByIdPort.findMcpTokenById("token-1"))
        .willReturn(Mono.just(token));

    StepVerifier.create(sut.getMcpToken(new GetMcpTokenQuery("token-1")))
        .expectNext(token)
        .verifyComplete();
  }

}
