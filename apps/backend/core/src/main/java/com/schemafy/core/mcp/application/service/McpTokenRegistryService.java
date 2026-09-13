package com.schemafy.core.mcp.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.mcp.application.port.in.GetMcpTokenQuery;
import com.schemafy.core.mcp.application.port.in.GetMcpTokenUseCase;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RegisterMcpTokenUseCase;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenCommand;
import com.schemafy.core.mcp.application.port.in.RevokeMcpTokenUseCase;
import com.schemafy.core.mcp.application.port.out.FindMcpTokenByIdPort;
import com.schemafy.core.mcp.application.port.out.RegisterMcpTokenPort;
import com.schemafy.core.mcp.application.port.out.RevokeMcpTokenPort;
import com.schemafy.core.mcp.domain.McpToken;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class McpTokenRegistryService implements
    RegisterMcpTokenUseCase,
    RevokeMcpTokenUseCase,
    GetMcpTokenUseCase {

  private final RegisterMcpTokenPort registerMcpTokenPort;
  private final RevokeMcpTokenPort revokeMcpTokenPort;
  private final FindMcpTokenByIdPort findMcpTokenByIdPort;

  @Override
  public Mono<McpToken> registerMcpToken(RegisterMcpTokenCommand command) {
    return registerMcpTokenPort.registerMcpToken(McpToken.issue(
        command.tokenId(),
        command.userId(),
        command.scope(),
        command.issuedAt(),
        command.expiresAt()));
  }

  @Override
  public Mono<Boolean> revokeMcpToken(RevokeMcpTokenCommand command) {
    return revokeMcpTokenPort.revokeMcpToken(
        command.tokenId(),
        command.userId(),
        command.revokedAt());
  }

  @Override
  public Mono<McpToken> getMcpToken(GetMcpTokenQuery query) {
    return findMcpTokenByIdPort.findMcpTokenById(query.tokenId());
  }

}
