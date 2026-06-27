package com.schemafy.core.mcp.domain;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpTokenClaimSupport")
class McpTokenClaimSupportTest {

  @Test
  @DisplayName("scope/scopes/scp 값을 공백, 콤마, 컬렉션 형식에서 추출한다")
  void extractsScopesFromSupportedClaims() {
    Map<String, Object> claims = Map.of(
        McpTokenClaimSupport.SCOPE, "mcp schema:read",
        McpTokenClaimSupport.SCOPES, "schema:write,mcp",
        McpTokenClaimSupport.SCP, List.of("project:read", " project:write "));

    assertThat(McpTokenClaimSupport.extractScopes(claims::get))
        .containsExactly("mcp", "schema:read", "schema:write",
            "project:read", "project:write");
  }

  @Test
  @DisplayName("blank scope 값은 무시한다")
  void ignoresBlankScopes() {
    assertThat(McpTokenClaimSupport.scopesFrom("  ", List.of("", "mcp")))
        .containsExactly("mcp");
  }

}
