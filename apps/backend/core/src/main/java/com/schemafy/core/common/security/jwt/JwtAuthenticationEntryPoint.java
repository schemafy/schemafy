package com.schemafy.core.common.security.jwt;

import com.schemafy.core.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final WebExchangeErrorWriter errorResponseWriter;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;
        return errorResponseWriter.writeErrorResponse(
                exchange,
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}
