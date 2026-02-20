package com.schemafy.core.common.security.jwt;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.schemafy.core.common.exception.AuthErrorCode;
import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint
    implements ServerAuthenticationEntryPoint {

  private final WebExchangeErrorWriter errorResponseWriter;

  @Override
  public Mono<Void> commence(ServerWebExchange exchange,
      AuthenticationException ex) {
    DomainErrorCode errorCode = AuthErrorCode.AUTHENTICATION_REQUIRED;
    return errorResponseWriter.writeErrorResponse(
        exchange,
        errorCode.status(),
        errorCode.code(),
        errorCode.code());
  }

}
