package com.schemafy.api.collaboration.service.handler;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.api.collaboration.dto.PreviewAction;
import com.schemafy.api.collaboration.dto.event.TablePositionPreviewEvent;
import com.schemafy.api.collaboration.service.CollaborationEventPublisher;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.api.collaboration.service.model.SessionEntry;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TablePositionPreviewMessageHandler 테스트")
class TablePositionPreviewMessageHandlerTest {

  @Mock
  private SessionRegistry sessionRegistry;

  @Mock
  private CollaborationEventPublisher eventPublisher;

  @Mock
  private SessionEntry sessionEntry;

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("유효한 UPDATE 메시지는 preview 이벤트를 발행한다")
  void handle_updateMessage_publishes_preview_event() {
    TablePositionPreviewMessageHandler handler = new TablePositionPreviewMessageHandler(
        sessionRegistry, eventPublisher);
    ObjectNode position = objectMapper.createObjectNode()
        .put("x", 120)
        .put("y", 80);
    TablePositionPreviewEvent.Inbound message = new TablePositionPreviewEvent.Inbound(
        PreviewAction.UPDATE, "schema-1", "table-1", position);
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.of(sessionEntry));
    given(eventPublisher.publish(eq("project-1"), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(handler.handle(MessageContext.of("project-1", "session-1"),
        message))
        .verifyComplete();

    ArgumentCaptor<TablePositionPreviewEvent.Outbound> captor = ArgumentCaptor.forClass(
        TablePositionPreviewEvent.Outbound.class);
    verify(eventPublisher).publish(eq("project-1"), captor.capture());
    assertThat(captor.getValue().action()).isEqualTo(PreviewAction.UPDATE);
    assertThat(captor.getValue().schemaId()).isEqualTo("schema-1");
    assertThat(captor.getValue().tableId()).isEqualTo("table-1");
    assertThat(captor.getValue().position()).isEqualTo(position);
  }

  @Test
  @DisplayName("CLEAR 메시지는 position payload를 무시하고 발행한다")
  void handle_clearMessage_ignores_position_payload() {
    TablePositionPreviewMessageHandler handler = new TablePositionPreviewMessageHandler(
        sessionRegistry, eventPublisher);
    ObjectNode position = objectMapper.createObjectNode().put("x", 1);
    TablePositionPreviewEvent.Inbound message = new TablePositionPreviewEvent.Inbound(
        PreviewAction.CLEAR, "schema-1", "table-1", position);
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.of(sessionEntry));
    given(eventPublisher.publish(eq("project-1"), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(handler.handle(MessageContext.of("project-1", "session-1"),
        message))
        .verifyComplete();

    ArgumentCaptor<TablePositionPreviewEvent.Outbound> captor = ArgumentCaptor.forClass(
        TablePositionPreviewEvent.Outbound.class);
    verify(eventPublisher).publish(eq("project-1"), captor.capture());
    assertThat(captor.getValue().action()).isEqualTo(PreviewAction.CLEAR);
    assertThat(captor.getValue().position()).isNull();
  }

  @Test
  @DisplayName("UPDATE 메시지의 position이 object가 아니면 무시한다")
  void handle_updateMessageWithInvalidPosition_ignores_message() {
    TablePositionPreviewMessageHandler handler = new TablePositionPreviewMessageHandler(
        sessionRegistry, eventPublisher);
    TablePositionPreviewEvent.Inbound message = new TablePositionPreviewEvent.Inbound(
        PreviewAction.UPDATE, "schema-1", "table-1",
        objectMapper.getNodeFactory().textNode("invalid"));
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.of(sessionEntry));

    StepVerifier.create(handler.handle(MessageContext.of("project-1", "session-1"),
        message))
        .verifyComplete();

    verify(eventPublisher, never()).publish(eq("project-1"), any());
  }

}
