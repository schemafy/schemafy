package com.schemafy.api.mcp.service;

import java.time.Duration;

import reactor.core.publisher.Mono;

public interface McpTokenRevocationCache {

  Mono<Void> cacheRevocation(String tokenId, Duration ttl);

}
