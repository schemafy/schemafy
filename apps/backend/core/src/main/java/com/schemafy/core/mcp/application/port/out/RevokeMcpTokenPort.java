package com.schemafy.core.mcp.application.port.out;

import java.time.Instant;

import reactor.core.publisher.Mono;

public interface RevokeMcpTokenPort {

  Mono<Boolean> revokeMcpToken(String tokenId, String userId, Instant revokedAt);

}
