package com.schemafy.core.common.security.jwt;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.ProblemDetailFactory;
import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WebExchangeErrorWriter {

  private final ObjectMapper objectMapper;
  private final ProblemDetailFactory problemDetailFactory;

  public Mono<Void> writeErrorResponse(ServerWebExchange exchange,
      DomainErrorCode errorCode, String errorMessage) {
    exchange.getResponse().setStatusCode(errorCode.status());
    exchange.getResponse().getHeaders()
        .setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    try {
      byte[] responseBytes = objectMapper.writeValueAsBytes(
          problemDetailFactory.create(exchange, errorCode,
              errorMessage));
      DataBuffer buffer = exchange.getResponse().bufferFactory()
          .wrap(responseBytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      return exchange.getResponse().setComplete();
    }
  }

}
