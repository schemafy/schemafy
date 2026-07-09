package com.schemafy.mcp.common.security;

import reactor.core.publisher.Mono;

public interface McpTokenRevocationCache {

  Mono<Boolean> isRevoked(String tokenId);

}
