package com.schemafy.core.mcp.application.port.out;

import com.schemafy.core.mcp.domain.McpToken;

import reactor.core.publisher.Mono;

public interface RegisterMcpTokenPort {

  Mono<McpToken> registerMcpToken(McpToken token);

}
