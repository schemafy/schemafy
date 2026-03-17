package com.schemafy.api.common.security.jwt;

import org.springframework.http.ProblemDetail;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.common.exception.ProblemDetailFactory;
import com.schemafy.core.common.exception.DomainErrorCode;
import com.schemafy.core.common.json.JsonCodec;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("WebExchangeErrorWriter")
class WebExchangeErrorWriterTest {

  @Test
  @DisplayName("JSON 직렬화에 실패하면 응답을 조용히 완료한다")
  void writeErrorResponse_completesWhenSerializationFails() {
    JsonCodec jsonCodec = Mockito.mock(JsonCodec.class);
    ProblemDetailFactory problemDetailFactory = Mockito.mock(
        ProblemDetailFactory.class);
    WebExchangeErrorWriter writer = new WebExchangeErrorWriter(jsonCodec,
        problemDetailFactory);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/test").build());

    when(problemDetailFactory.create(any(), any(DomainErrorCode.class),
        anyString())).thenReturn(ProblemDetail.forStatus(500));
    when(jsonCodec.serializeBytes(any()))
        .thenThrow(new IllegalArgumentException("boom"));

    StepVerifier.create(writer.writeErrorResponse(exchange,
        CommonErrorCode.INTERNAL_SERVER_ERROR, "boom"))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode())
        .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.status());
  }

}
