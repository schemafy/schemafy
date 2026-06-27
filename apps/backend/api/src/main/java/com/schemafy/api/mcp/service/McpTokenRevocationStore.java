package com.schemafy.api.mcp.service;

import java.time.Duration;

import reactor.core.publisher.Mono;

public interface McpTokenRevocationStore {

  Mono<Void> revoke(String tokenId, Duration ttl);

}
