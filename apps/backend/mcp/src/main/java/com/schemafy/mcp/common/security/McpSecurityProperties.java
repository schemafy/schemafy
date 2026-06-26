package com.schemafy.mcp.common.security;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcp.security")
public class McpSecurityProperties {

  @Valid
  private Token token = new Token();

  @Valid
  private RateLimit rateLimit = new RateLimit();

  public Token getToken() { return token; }

  public void setToken(Token token) { this.token = token; }

  public RateLimit getRateLimit() { return rateLimit; }

  public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

  public static class Token {

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

    @PositiveOrZero
    private long clockSkewSeconds = 30;

    public String getSecret() { return secret; }

    public void setSecret(String secret) { this.secret = secret; }

    public String getIssuer() { return issuer; }

    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAudience() { return audience; }

    public void setAudience(String audience) { this.audience = audience; }

    public String getTokenType() { return tokenType; }

    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getRequiredScope() { return requiredScope; }

    public void setRequiredScope(String requiredScope) { this.requiredScope = requiredScope; }

    public String getRevocationKeyPrefix() { return revocationKeyPrefix; }

    public void setRevocationKeyPrefix(String revocationKeyPrefix) { this.revocationKeyPrefix = revocationKeyPrefix; }

    public long getClockSkewSeconds() { return clockSkewSeconds; }

    public void setClockSkewSeconds(long clockSkewSeconds) { this.clockSkewSeconds = clockSkewSeconds; }

  }

  public static class RateLimit {

    private boolean enabled = true;

    @Positive
    private int requests = 120;

    @NotBlank
    private String keyPrefix = "mcp:rate-limit:user:";

    private Duration window = Duration.ofMinutes(1);

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getRequests() { return requests; }

    public void setRequests(int requests) { this.requests = requests; }

    public String getKeyPrefix() { return keyPrefix; }

    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public Duration getWindow() { return window; }

    public void setWindow(Duration window) { this.window = window; }

  }

}
