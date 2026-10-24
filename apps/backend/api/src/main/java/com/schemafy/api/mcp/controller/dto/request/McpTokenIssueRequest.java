package com.schemafy.api.mcp.controller.dto.request;

import java.util.Set;

public record McpTokenIssueRequest(
    Set<String> scopes) {
}
