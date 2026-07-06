package com.schemafy.api.mcp.docs;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.api.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;

/** MCP Token API 문서화를 위한 스니펫 제공 클래스 */
public class McpTokenApiSnippets extends RestDocsSnippets {

  public static Snippet issueMcpTokenRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  public static Snippet issueMcpTokenResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet issueMcpTokenResponse() {
    return createResponseFieldsSnippet(
        fieldWithPath("token").type(JsonFieldType.STRING)
            .description("MCP 서버 요청에 사용할 Bearer 토큰"),
        fieldWithPath("tokenType").type(JsonFieldType.STRING)
            .description("토큰 타입 (고정값: Bearer)"),
        fieldWithPath("expiresInSeconds").type(JsonFieldType.NUMBER)
            .description("토큰 만료까지 남은 시간(초)"));
  }

  public static Snippet revokeMcpTokenRequestHeaders() {
    return createRequestHeadersSnippet(authorizationHeader());
  }

  public static Snippet revokeMcpTokenRequest() {
    return requestFields(
        fieldWithPath("token").type(JsonFieldType.STRING)
            .description("폐기할 MCP Bearer 토큰"));
  }

}
