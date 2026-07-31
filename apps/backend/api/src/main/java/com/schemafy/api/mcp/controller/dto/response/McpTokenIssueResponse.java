package com.schemafy.api.mcp.controller.dto.response;

import com.schemafy.api.mcp.service.McpTokenIssueResult;

public record McpTokenIssueResponse(
    String token,
    String tokenType,
    long expiresInSeconds) {

  public static McpTokenIssueResponse from(McpTokenIssueResult result) {
    return new McpTokenIssueResponse(
        result.token(),
        "Bearer",
        result.expiresInSeconds());
  }

}
