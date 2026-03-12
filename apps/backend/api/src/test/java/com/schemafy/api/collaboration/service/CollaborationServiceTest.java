package com.schemafy.api.collaboration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.collaboration.dto.BroadcastMessage;
import com.schemafy.api.collaboration.dto.CursorPosition;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.dto.event.CursorEvent;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationService 단위 테스트")
class CollaborationServiceTest {

  @Mock
  private SessionRegistry sessionRegistry;

  @Mock
  private CollaborationEventPublisher eventPublisher;

  private ObjectMapper objectMapper;
  private CollaborationService collaborationService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    CollaborationPayloadSerializer serializer = new CollaborationPayloadSerializer(
        objectMapper);
    collaborationService = new CollaborationService(sessionRegistry,
        eventPublisher, serializer, objectMapper, java.util.List.of());
  }

  @Test
  @DisplayName("CURSOR 이벤트는 sender를 제외하고 브로드캐스트한다")
  void handleRedisMessage_excludes_sender_for_cursor_event()
      throws Exception {
    String message = objectMapper.writeValueAsString(
        CollaborationOutboundFactory.cursor("session-1",
            new CursorEvent.UserInfo(
                "user-1", "tester"),
            new CursorPosition(1.0,
                2.0)));

    StepVerifier.create(
        collaborationService.handleRedisMessage("project-1", message))
        .verifyComplete();

    ArgumentCaptor<BroadcastMessage> captor = ArgumentCaptor.forClass(
        BroadcastMessage.class);
    verify(sessionRegistry).broadcast(captor.capture());
    assertThat(captor.getValue().projectId()).isEqualTo("project-1");
    assertThat(captor.getValue().excludeSessionId())
        .isEqualTo("session-1");
    assertThat(captor.getValue().message())
        .contains("\"type\":\"CURSOR\"");
    assertThat(captor.getValue().message()).doesNotContain("sessionId");
  }

  @Test
  @DisplayName("CHAT 이벤트는 sender를 포함해서 브로드캐스트한다")
  void handleRedisMessage_includes_sender_for_chat_event() throws Exception {
    String message = objectMapper.writeValueAsString(
        CollaborationOutboundFactory.chat("session-1", "message-1",
            "user-1", "tester", "hello"));

    StepVerifier.create(
        collaborationService.handleRedisMessage("project-1", message))
        .verifyComplete();

    ArgumentCaptor<BroadcastMessage> captor = ArgumentCaptor.forClass(
        BroadcastMessage.class);
    verify(sessionRegistry).broadcast(captor.capture());
    assertThat(captor.getValue().excludeSessionId()).isNull();
    assertThat(captor.getValue().message()).contains("\"type\":\"CHAT\"");
    assertThat(captor.getValue().message()).doesNotContain("sessionId");
  }

  @Test
  @DisplayName("SESSION_READY 이벤트는 Redis 브로드캐스트를 무시한다")
  void handleRedisMessage_ignores_session_ready_event() throws Exception {
    String message = objectMapper.writeValueAsString(
        CollaborationOutboundFactory.sessionReady("session-1"));

    StepVerifier.create(
        collaborationService.handleRedisMessage("project-1", message))
        .verifyComplete();

    verify(sessionRegistry, never()).broadcast(org.mockito.ArgumentMatchers.any());
  }

}
