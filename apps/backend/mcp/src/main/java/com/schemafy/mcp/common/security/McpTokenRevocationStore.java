package com.schemafy.mcp.common.security;

import reactor.core.publisher.Mono;

public interface McpTokenRevocationStore {

  Mono<Boolean> isRevoked(String tokenId);

}
