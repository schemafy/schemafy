package com.schemafy.core.mcp.application.port.in;

import reactor.core.publisher.Mono;

public interface RevokeMcpTokenUseCase {

  Mono<Boolean> revokeMcpToken(RevokeMcpTokenCommand command);

}
