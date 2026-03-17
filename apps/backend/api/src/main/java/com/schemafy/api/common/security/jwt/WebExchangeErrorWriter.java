package com.schemafy.api.common.security.jwt;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.schemafy.api.common.exception.ProblemDetailFactory;
import com.schemafy.core.common.exception.DomainErrorCode;
import com.schemafy.core.common.json.JsonCodec;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WebExchangeErrorWriter {

  private final JsonCodec jsonCodec;
  private final ProblemDetailFactory problemDetailFactory;

  public Mono<Void> writeErrorResponse(ServerWebExchange exchange,
      DomainErrorCode errorCode, String errorMessage) {
    exchange.getResponse().setStatusCode(errorCode.status());
    exchange.getResponse().getHeaders()
        .setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    try {
      byte[] responseBytes = jsonCodec.serializeBytes(
          problemDetailFactory.create(exchange, errorCode,
              errorMessage));
      DataBuffer buffer = exchange.getResponse().bufferFactory()
          .wrap(responseBytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (IllegalArgumentException e) {
      return exchange.getResponse().setComplete();
    }
  }

}
