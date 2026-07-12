package com.schemafy.core.mcp.adapter.out.persistence;

import java.time.Instant;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.mcp.application.port.out.FindMcpTokenByIdPort;
import com.schemafy.core.mcp.application.port.out.RegisterMcpTokenPort;
import com.schemafy.core.mcp.application.port.out.RevokeMcpTokenPort;
import com.schemafy.core.mcp.domain.McpToken;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class McpTokenPersistenceAdapter implements
    RegisterMcpTokenPort,
    FindMcpTokenByIdPort,
    RevokeMcpTokenPort {

  private final McpTokenRepository mcpTokenRepository;

  @Override
  public Mono<McpToken> registerMcpToken(McpToken token) {
    return mcpTokenRepository.save(token);
  }

  @Override
  public Mono<McpToken> findMcpTokenById(String tokenId) {
    return mcpTokenRepository.findById(tokenId);
  }

  @Override
  public Mono<Boolean> revokeMcpToken(
      String tokenId,
      String userId,
      Instant revokedAt) {
    return mcpTokenRepository.findByIdAndUserIdAndDeletedAtIsNull(tokenId, userId)
        .flatMap(token -> {
          if (token.isRevoked()) {
            return Mono.just(true);
          }
          token.revoke(revokedAt);
          return mcpTokenRepository.save(token).thenReturn(true);
        })
        .defaultIfEmpty(false);
  }

}
