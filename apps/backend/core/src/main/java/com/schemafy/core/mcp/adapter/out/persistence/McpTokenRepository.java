package com.schemafy.core.mcp.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.mcp.domain.McpToken;

import reactor.core.publisher.Mono;

interface McpTokenRepository extends ReactiveCrudRepository<McpToken, String> {

  Mono<McpToken> findByIdAndUserIdAndDeletedAtIsNull(
      String id,
      String userId);

}
