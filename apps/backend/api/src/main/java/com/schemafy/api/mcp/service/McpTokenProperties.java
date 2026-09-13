package com.schemafy.api.mcp.service;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "mcp.security.token")
public class McpTokenProperties {

  @NotBlank
  @Size(min = 32)
  private String secret = "schemafy-mcp-local-secret-minimum-256-bit-key-value";

  @NotBlank
  private String issuer = "schemafy-mcp";

  @NotBlank
  private String audience = "schemafy-mcp";

  @NotBlank
  private String tokenType = "MCP";

  @NotBlank
  private String requiredScope = "mcp";

  @NotBlank
  private String revocationKeyPrefix = "mcp:token:revoked:";

  @NotNull
  private Duration expiresIn = Duration.ofDays(7);

}
