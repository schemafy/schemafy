package com.schemafy.api.collaboration.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationDirectMessageSender 테스트")
class CollaborationDirectMessageSenderTest {

  @Mock
  private SessionEntry entry;

  private final CollaborationDirectMessageSender sender = new CollaborationDirectMessageSender(
      new CollaborationPayloadSerializer(
          new JsonCodec(new ObjectMapper().findAndRegisterModules())));

  @Test
  @DisplayName("SESSION_READY를 현재 세션에 직접 전송한다")
  void sendSessionReady_emits_serialized_payload() {
    given(entry.send(anyString())).willReturn(Sinks.EmitResult.OK);

    StepVerifier.create(sender.sendSessionReady(entry, "session-1"))
        .verifyComplete();

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(
        String.class);
    verify(entry).send(payloadCaptor.capture());
    assertThat(payloadCaptor.getValue())
        .contains("\"type\":\"SESSION_READY\"");
    assertThat(payloadCaptor.getValue())
        .contains("\"sessionId\":\"session-1\"");
  }

  @Test
  @DisplayName("세션 큐 적재 실패 시 에러를 반환한다")
  void sendSessionReady_returns_error_when_emit_fails() {
    given(entry.send(anyString())).willReturn(Sinks.EmitResult.FAIL_TERMINATED);

    StepVerifier.create(sender.sendSessionReady(entry, "session-1"))
        .expectErrorSatisfies(error -> assertThat(error)
            .hasMessageContaining("FAIL_TERMINATED"))
        .verify();
  }

}
