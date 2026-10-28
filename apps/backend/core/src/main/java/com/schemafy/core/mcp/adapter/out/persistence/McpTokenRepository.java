package com.schemafy.core.mcp.adapter.out.persistence;

import java.time.Instant;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.mcp.domain.McpToken;

import reactor.core.publisher.Mono;

interface McpTokenRepository extends ReactiveCrudRepository<McpToken, String> {

  Mono<McpToken> findByIdAndUserIdAndDeletedAtIsNull(
      String id,
      String userId);

  @Modifying
  @Query("""
      UPDATE mcp_tokens
      SET revoked_at = COALESCE(revoked_at, :revokedAt),
          updated_at = CURRENT_TIMESTAMP
      WHERE user_id = :userId
        AND deleted_at IS NULL
      """)
  Mono<Long> revokeAllByUserId(String userId, Instant revokedAt);

}
