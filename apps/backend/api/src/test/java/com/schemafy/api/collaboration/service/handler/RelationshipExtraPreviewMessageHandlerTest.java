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
import com.schemafy.api.collaboration.dto.event.RelationshipExtraPreviewEvent;
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
@DisplayName("RelationshipExtraPreviewMessageHandler 테스트")
class RelationshipExtraPreviewMessageHandlerTest {

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
    RelationshipExtraPreviewMessageHandler handler = new RelationshipExtraPreviewMessageHandler(
        sessionRegistry, eventPublisher);
    ObjectNode extra = objectMapper.createObjectNode()
        .put("fkHandle", "right")
        .put("pkHandle", "left");
    RelationshipExtraPreviewEvent.Inbound message = new RelationshipExtraPreviewEvent.Inbound(
        PreviewAction.UPDATE, "schema-1", "rel-1", extra);
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.of(sessionEntry));
    given(eventPublisher.publish(eq("project-1"), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(handler.handle(MessageContext.of("project-1", "session-1"),
        message))
        .verifyComplete();

    ArgumentCaptor<RelationshipExtraPreviewEvent.Outbound> captor = ArgumentCaptor.forClass(
        RelationshipExtraPreviewEvent.Outbound.class);
    verify(eventPublisher).publish(eq("project-1"), captor.capture());
    assertThat(captor.getValue().action()).isEqualTo(PreviewAction.UPDATE);
    assertThat(captor.getValue().schemaId()).isEqualTo("schema-1");
    assertThat(captor.getValue().relationshipId()).isEqualTo("rel-1");
    assertThat(captor.getValue().extra()).isEqualTo(extra);
  }

  @Test
  @DisplayName("CLEAR 메시지는 extra payload를 무시하고 발행한다")
  void handle_clearMessage_ignores_extra_payload() {
    RelationshipExtraPreviewMessageHandler handler = new RelationshipExtraPreviewMessageHandler(
        sessionRegistry, eventPublisher);
    ObjectNode extra = objectMapper.createObjectNode().put("fkHandle", "right");
    RelationshipExtraPreviewEvent.Inbound message = new RelationshipExtraPreviewEvent.Inbound(
        PreviewAction.CLEAR, "schema-1", "rel-1", extra);
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.of(sessionEntry));
    given(eventPublisher.publish(eq("project-1"), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(handler.handle(MessageContext.of("project-1", "session-1"),
        message))
        .verifyComplete();

    ArgumentCaptor<RelationshipExtraPreviewEvent.Outbound> captor = ArgumentCaptor.forClass(
        RelationshipExtraPreviewEvent.Outbound.class);
    verify(eventPublisher).publish(eq("project-1"), captor.capture());
    assertThat(captor.getValue().action()).isEqualTo(PreviewAction.CLEAR);
    assertThat(captor.getValue().extra()).isNull();
  }

  @Test
  @DisplayName("세션이 없으면 메시지를 무시한다")
  void handle_withoutSession_ignores_message() {
    RelationshipExtraPreviewMessageHandler handler = new RelationshipExtraPreviewMessageHandler(
        sessionRegistry, eventPublisher);
    RelationshipExtraPreviewEvent.Inbound message = new RelationshipExtraPreviewEvent.Inbound(
        PreviewAction.UPDATE, "schema-1", "rel-1",
        objectMapper.createObjectNode());
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.empty());

    StepVerifier.create(handler.handle(MessageContext.of("project-1", "session-1"),
        message))
        .verifyComplete();

    verify(eventPublisher, never()).publish(eq("project-1"), any());
  }

}
