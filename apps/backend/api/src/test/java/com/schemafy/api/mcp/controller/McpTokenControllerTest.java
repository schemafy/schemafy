package com.schemafy.api.mcp.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.mcp.docs.McpTokenApiSnippets;
import com.schemafy.api.mcp.exception.McpTokenErrorCode;
import com.schemafy.api.mcp.service.McpTokenRevocationCache;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("McpTokenController 통합 테스트")
class McpTokenControllerTest extends UserHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.API.replace("{version}",
      "v1.0");

  @Autowired
  WebTestClient webTestClient;

  @Autowired
  TestMcpTokenRevocationCache revocationCache;

  @BeforeEach
  void setUp() {
    databaseClient.sql("DELETE FROM mcp_tokens")
        .fetch()
        .rowsUpdated()
        .then(cleanupUserFixtures())
        .block();
    revocationCache.clear();
  }

  @Test
  @DisplayName("인증된 사용자는 MCP 토큰을 발급받는다")
  void issuesMcpToken() {
    User user = createUser("mcp-issue@example.com", "MCP User");
    String accessToken = generateAccessToken(user.id());

    EntityExchangeResult<byte[]> result = webTestClient.post()
        .uri(API_BASE_PATH + "/mcp/tokens")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .consumeWith(document("mcp-token-issue",
            McpTokenApiSnippets.issueMcpTokenRequestHeaders(),
            McpTokenApiSnippets.issueMcpTokenResponseHeaders(),
            McpTokenApiSnippets.issueMcpTokenResponse()))
        .jsonPath("$.token").isNotEmpty()
        .jsonPath("$.tokenType").isEqualTo("Bearer")
        .jsonPath("$.expiresInSeconds").isEqualTo(900)
        .jsonPath("$.tokenId").doesNotExist()
        .jsonPath("$.scope").doesNotExist()
        .jsonPath("$.issuedAt").doesNotExist()
        .jsonPath("$.expiresAt").doesNotExist()
        .returnResult();

    McpTokenRow tokenRow = findTokenRowByUserId(user.id());
    assertThat(tokenRow.userId()).isEqualTo(user.id());
    assertThat(tokenRow.scope()).isEqualTo("mcp");
    assertThat(tokenRow.revokedAt()).isNull();
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 MCP 토큰을 발급받을 수 없다")
  void rejectsIssueWhenUnauthenticated() {
    webTestClient.post()
        .uri(API_BASE_PATH + "/mcp/tokens")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("사용자 자신의 MCP 토큰을 revoke한다")
  void revokesOwnMcpToken() {
    User user = createUser("mcp-revoke@example.com", "MCP Revoke User");
    String accessToken = generateAccessToken(user.id());
    EntityExchangeResult<byte[]> issueResult = issueToken(accessToken);
    String responseBody = new String(issueResult.getResponseBody());
    String token = JsonPath.read(responseBody, "$.token");
    McpTokenRow issuedTokenRow = findTokenRowByUserId(user.id());

    webTestClient.post()
        .uri(API_BASE_PATH + "/mcp/tokens/revoke")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("token", token))
        .exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .consumeWith(document("mcp-token-revoke",
            McpTokenApiSnippets.revokeMcpTokenRequestHeaders(),
            McpTokenApiSnippets.revokeMcpTokenRequest()))
        .isEmpty();

    assertThat(revocationCache.revokedTokens())
        .singleElement()
        .satisfies(revoked -> assertThat(revoked.tokenId()).isEqualTo(issuedTokenRow.id()));
    assertThat(findTokenRow(issuedTokenRow.id()).revokedAt()).isNotNull();
  }

  @Test
  @DisplayName("다른 사용자의 MCP 토큰 revoke는 거부한다")
  void rejectsRevokingAnotherUsersToken() {
    User userA = createUser("mcp-owner-a@example.com", "MCP Owner A");
    User userB = createUser("mcp-owner-b@example.com", "MCP Owner B");
    String userAToken = generateAccessToken(userA.id());
    String userBToken = generateAccessToken(userB.id());
    EntityExchangeResult<byte[]> issueResult = issueToken(userAToken);
    String mcpToken = JsonPath.read(new String(issueResult.getResponseBody()),
        "$.token");

    webTestClient.post()
        .uri(API_BASE_PATH + "/mcp/tokens/revoke")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("token", mcpToken))
        .exchange()
        .expectStatus().isForbidden()
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo(McpTokenErrorCode.OWNER_MISMATCH.code());

    assertThat(revocationCache.revokedTokens()).isEmpty();
  }

  private EntityExchangeResult<byte[]> issueToken(String accessToken) {
    return webTestClient.post()
        .uri(API_BASE_PATH + "/mcp/tokens")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .returnResult();
  }

  private McpTokenRow findTokenRow(String tokenId) {
    return databaseClient.sql("""
        SELECT id, user_id, scope, revoked_at
        FROM mcp_tokens
        WHERE id = :tokenId
        """)
        .bind("tokenId", tokenId)
        .map((row, metadata) -> new McpTokenRow(
            row.get("id", String.class),
            row.get("user_id", String.class),
            row.get("scope", String.class),
            row.get("revoked_at")))
        .one()
        .block();
  }

  private McpTokenRow findTokenRowByUserId(String userId) {
    return databaseClient.sql("""
        SELECT id, user_id, scope, revoked_at
        FROM mcp_tokens
        WHERE user_id = :userId
        """)
        .bind("userId", userId)
        .map((row, metadata) -> new McpTokenRow(
            row.get("id", String.class),
            row.get("user_id", String.class),
            row.get("scope", String.class),
            row.get("revoked_at")))
        .one()
        .block();
  }

  @TestConfiguration
  static class McpTokenControllerTestConfig {

    @Bean
    TestMcpTokenRevocationCache testMcpTokenRevocationCache() {
      return new TestMcpTokenRevocationCache();
    }

  }

  static class TestMcpTokenRevocationCache
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

    void clear() {
      revokedTokens.clear();
    }

  }

  record RevokedToken(String tokenId, Duration ttl) {
  }

  record McpTokenRow(String id, String userId, String scope, Object revokedAt) {
  }

}
