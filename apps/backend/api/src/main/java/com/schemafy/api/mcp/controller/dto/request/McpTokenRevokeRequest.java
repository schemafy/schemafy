package com.schemafy.api.mcp.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record McpTokenRevokeRequest(
    @NotBlank String token) {
}
