package com.schemafy.mcp.common.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class McpSecurityIntegrationTest {

  @Autowired
  WebTestClient webTestClient;

  @Autowired
  McpSecurityProperties properties;

  McpTokenTestFactory tokenFactory;

  @BeforeEach
  void setUp() {
    tokenFactory = new McpTokenTestFactory(properties);
  }

  @Test
  @DisplayName("/mcp 요청은 Authorization 헤더가 없으면 401을 반환한다")
  void rejectsMissingToken() {
    webTestClient.post()
        .uri("/mcp")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(initializeRequest())
        .exchange()
        .expectStatus().isUnauthorized()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.reason").isEqualTo("MCP_TOKEN_MISSING")
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.title").isEqualTo("Unauthorized")
        .jsonPath("$.detail").isEqualTo("MCP Bearer token is required")
        .jsonPath("$.instance").isEqualTo("/mcp");
  }

  @Test
  @DisplayName("/mcp 요청은 잘못된 토큰이면 401을 반환한다")
  void rejectsInvalidToken() {
    webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(initializeRequest())
        .exchange()
        .expectStatus().isUnauthorized()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.reason").isEqualTo("MCP_TOKEN_MALFORMED")
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.title").isEqualTo("Unauthorized")
        .jsonPath("$.detail").isEqualTo("MCP token is malformed")
        .jsonPath("$.instance").isEqualTo("/mcp");
  }

  @Test
  @DisplayName("/mcp 요청은 MCP scope가 부족하면 403을 반환한다")
  void rejectsInsufficientScope() {
    webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFactory.tokenWithoutRequiredScope())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(initializeRequest())
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.reason").isEqualTo("MCP_INSUFFICIENT_SCOPE")
        .jsonPath("$.status").isEqualTo(403)
        .jsonPath("$.title").isEqualTo("Forbidden")
        .jsonPath("$.detail").isEqualTo("MCP token scope is insufficient")
        .jsonPath("$.instance").isEqualTo("/mcp");
  }

  @Test
  @DisplayName("유효한 MCP 토큰은 requesterId와 SecurityContext를 Reactor context에 저장한다")
  void storesRequesterContext() {
    webTestClient.post()
        .uri("/mcp/context-probe")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFactory.validToken())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.requesterId").isEqualTo("user-1")
        .jsonPath("$.actorUserId").isEqualTo("user-1")
        .jsonPath("$.authenticationName").isEqualTo("user-1");
  }

  @Test
  @DisplayName("Authorization Bearer prefix는 대소문자를 구분하지 않는다")
  void acceptsLowercaseBearerPrefix() {
    webTestClient.post()
        .uri("/mcp/context-probe")
        .header(HttpHeaders.AUTHORIZATION, "bearer " + tokenFactory.validToken())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.requesterId").isEqualTo("user-1")
        .jsonPath("$.actorUserId").isEqualTo("user-1")
        .jsonPath("$.authenticationName").isEqualTo("user-1");
  }

  @Test
  @DisplayName("유효한 MCP 토큰이면 initialize 요청이 성공하고 빈 tool 목록 상태를 유지한다")
  void initializesWithValidToken() {
    String token = tokenFactory.validToken();
    EntityExchangeResult<byte[]> initializeResult = webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(initializeRequest())
        .exchange()
        .expectStatus().isOk()
        .expectHeader().exists("Mcp-Session-Id")
        .expectBody()
        .returnResult();

    String sessionId = initializeResult.getResponseHeaders().getFirst("Mcp-Session-Id");
    assertThat(new String(initializeResult.getResponseBody()))
        .contains("schemafy-mcp-test");

    webTestClient.post()
        .uri("/mcp")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .header("Mcp-Session-Id", sessionId)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .bodyValue(mcpRequest("tools-1", "tools/list"))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(result -> assertThat(new String(result.getResponseBody()))
            .contains("\"tools\":[]"));
  }

  private Map<String, Object> initializeRequest() {
    return Map.of(
        "jsonrpc", "2.0",
        "id", "init-1",
        "method", "initialize",
        "params", Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(),
            "clientInfo", Map.of(
                "name", "schemafy-test-client",
                "version", "0.0.1")));
  }

  private Map<String, Object> mcpRequest(String id, String method) {
    return Map.of(
        "jsonrpc", "2.0",
        "id", id,
        "method", method,
        "params", Map.of());
  }

  @TestConfiguration
  static class ContextProbeConfiguration {

    @Bean
    McpTokenRevocationStore tokenRevocationStore() {
      return tokenId -> Mono.just(false);
    }

    @Bean
    McpRateLimiter rateLimiter() {
      return claims -> Mono.just(true);
    }

    @Bean
    RouterFunction<ServerResponse> contextProbeRoute() {
      return RouterFunctions.route(POST("/mcp/context-probe"),
          request -> Mono.deferContextual(context -> ReactiveSecurityContextHolder.getContext()
              .flatMap(securityContext -> ServerResponse.ok().bodyValue(Map.of(
                  "requesterId", ProjectAccessRequesterContext.requesterIdOrNull(context),
                  "actorUserId", ErdOperationContexts.metadata(context).actorUserIdOr(null),
                  "authenticationName", securityContext.getAuthentication().getName())))));
    }

  }

}
