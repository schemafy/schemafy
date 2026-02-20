package com.schemafy.core.common.security.jwt;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.schemafy.core.common.exception.AuthErrorCode;
import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements ServerAccessDeniedHandler {

  private final WebExchangeErrorWriter errorResponseWriter;

  @Override
  public Mono<Void> handle(ServerWebExchange exchange,
      AccessDeniedException denied) {
    DomainErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;
    return errorResponseWriter.writeErrorResponse(
        exchange,
        errorCode.status(),
        errorCode.code(),
        errorCode.code());
  }

}
