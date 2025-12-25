package com.schemafy.core.common.security.jwt;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.type.BaseResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WebExchangeErrorWriter {

  private final ObjectMapper objectMapper;

  public Mono<Void> writeErrorResponse(ServerWebExchange exchange,
      HttpStatus httpStatus, String errorCode, String errorMessage) {
    exchange.getResponse().setStatusCode(httpStatus);
    exchange.getResponse().getHeaders()
        .setContentType(MediaType.APPLICATION_JSON);

    BaseResponse<Void> errorResponse = BaseResponse.error(errorCode,
        errorMessage);

    try {
      byte[] responseBytes = objectMapper
          .writeValueAsBytes(errorResponse);
      DataBuffer buffer = exchange.getResponse().bufferFactory()
          .wrap(responseBytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      return exchange.getResponse().setComplete();
    }
  }

}
