package com.schemafy.core.common.security.jwt;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.schemafy.core.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements ServerAccessDeniedHandler {

  private final WebExchangeErrorWriter errorResponseWriter;

  @Override
  public Mono<Void> handle(ServerWebExchange exchange,
      AccessDeniedException denied) {
    ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
    return errorResponseWriter.writeErrorResponse(
        exchange,
        errorCode.getStatus(),
        errorCode.getCode(),
        errorCode.getMessage());
  }

}
