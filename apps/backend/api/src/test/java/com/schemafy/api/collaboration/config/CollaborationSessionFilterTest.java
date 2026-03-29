package com.schemafy.api.collaboration.config;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationSessionFilter")
class CollaborationSessionFilterTest {

  private final CollaborationSessionFilter sut = new CollaborationSessionFilter();

  @Mock
  WebFilterChain filterChain;

  @Test
  @DisplayName("협업 헤더를 typed metadata와 브로드캐스트 session context에 함께 저장한다")
  void storesHeadersInTypedMetadataAndBroadcastSessionContext() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test")
        .header(CollaborationConstants.SESSION_ID_HEADER, "session-1")
        .header(CollaborationConstants.CLIENT_OPERATION_ID_HEADER, "client-op-1")
        .header(CollaborationConstants.BASE_SCHEMA_REVISION_HEADER, "7")
        .build());
    AtomicReference<ErdOperationMetadata> metadataRef = new AtomicReference<>();
    AtomicReference<String> sessionIdRef = new AtomicReference<>();

    when(filterChain.filter(any())).thenReturn(Mono.deferContextual(ctx -> {
      metadataRef.set(ErdOperationContexts.metadata(ctx));
      sessionIdRef.set(ctx.getOrDefault(CollaborationConstants.SESSION_ID_CONTEXT_KEY, null));
      return Mono.empty();
    }));

    StepVerifier.create(sut.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(metadataRef.get()).isEqualTo(new ErdOperationMetadata(
        "session-1",
        "client-op-1",
        7L,
        null));
    assertThat(sessionIdRef.get()).isEqualTo("session-1");
  }

  @Test
  @DisplayName("base schema revision 헤더가 숫자가 아니면 metadata에 넣지 않는다")
  void ignoresInvalidBaseSchemaRevisionHeader() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test")
        .header(CollaborationConstants.BASE_SCHEMA_REVISION_HEADER, "invalid")
        .build());
    AtomicReference<ErdOperationMetadata> metadataRef = new AtomicReference<>();

    when(filterChain.filter(any())).thenReturn(Mono.deferContextual(ctx -> {
      metadataRef.set(ErdOperationContexts.metadata(ctx));
      return Mono.empty();
    }));

    StepVerifier.create(sut.filter(exchange, filterChain))
        .verifyComplete();

    assertThat(metadataRef.get()).isEqualTo(ErdOperationMetadata.empty());
  }

}
