package com.schemafy.core.mcp.application.port.in;

import com.schemafy.core.mcp.domain.McpToken;

import reactor.core.publisher.Mono;

public interface RegisterMcpTokenUseCase {

  Mono<McpToken> registerMcpToken(RegisterMcpTokenCommand command);

}
