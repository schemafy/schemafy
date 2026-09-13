package com.schemafy.core.mcp.domain;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("mcp_tokens")
public class McpToken extends BaseEntity {

  private String userId;
  private String scope;
  private Instant issuedAt;
  private Instant expiresAt;
  private Instant revokedAt;
  private Instant lastUsedAt;

  public static McpToken issue(
      String tokenId,
      String userId,
      String scope,
      Instant issuedAt,
      Instant expiresAt) {
    McpToken token = new McpToken(
        userId,
        scope,
        issuedAt,
        expiresAt,
        null,
        null);
    token.setId(tokenId);
    return token;
  }

  public boolean belongsTo(String userId) {
    return this.userId != null && this.userId.equals(userId);
  }

  public boolean hasScope(String scope) {
    return this.scope != null && this.scope.equals(scope);
  }

  public boolean isExpiredAt(Instant now) {
    return expiresAt == null || !expiresAt.isAfter(now);
  }

  public boolean isRevoked() { return revokedAt != null; }

  public void revoke(Instant revokedAt) {
    if (this.revokedAt == null) {
      this.revokedAt = revokedAt;
    }
  }

}
