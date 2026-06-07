package com.schemafy.api.collaboration.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.api.collaboration.dto.BroadcastMessage;
import com.schemafy.api.collaboration.dto.CursorPosition;
import com.schemafy.api.collaboration.dto.PreviewAction;
import com.schemafy.api.collaboration.dto.ProjectPresenceParticipant;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.dto.event.CursorEvent;
import com.schemafy.api.collaboration.security.WebSocketAuthInfo;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.api.collaboration.service.presence.ProjectPresenceSession;
import com.schemafy.api.collaboration.service.presence.ProjectPresenceStore;
import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationService 단위 테스트")
class CollaborationServiceTest {

  @Mock
  private SessionRegistry sessionRegistry;

  @Mock
  private CollaborationEventPublisher eventPublisher;

  @Mock
  private ProjectPresenceStore presenceStore;

  private ObjectMapper objectMapper;
  private CollaborationService collaborationService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    JsonCodec jsonCodec = new JsonCodec(objectMapper);
    CollaborationPayloadSerializer serializer = new CollaborationPayloadSerializer(
        jsonCodec);
    collaborationService = new CollaborationService(sessionRegistry,
        eventPublisher, serializer, presenceStore, jsonCodec,
        List.of());
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
    assertThat(captor.getValue().message())
        .contains("\"sessionId\":\"session-1\"");
  }

  @Test
  @DisplayName("TABLE_POSITION_PREVIEW 이벤트는 sender를 제외하고 브로드캐스트한다")
  void handleRedisMessage_excludes_sender_for_table_position_preview()
      throws Exception {
    ObjectNode position = objectMapper.createObjectNode()
        .put("x", 120)
        .put("y", 80);
    String message = objectMapper.writeValueAsString(
        CollaborationOutboundFactory.tablePositionPreview("session-1",
            PreviewAction.UPDATE, "schema-1", "table-1", position));

    StepVerifier.create(
        collaborationService.handleRedisMessage("project-1", message))
        .verifyComplete();

    ArgumentCaptor<BroadcastMessage> captor = ArgumentCaptor.forClass(
        BroadcastMessage.class);
    verify(sessionRegistry).broadcast(captor.capture());
    assertThat(captor.getValue().excludeSessionId())
        .isEqualTo("session-1");
    assertThat(captor.getValue().message())
        .contains("\"type\":\"TABLE_POSITION_PREVIEW\"");
    assertThat(captor.getValue().message())
        .contains("\"action\":\"UPDATE\"");
  }

  @Test
  @DisplayName("RELATIONSHIP_EXTRA_PREVIEW 이벤트는 sender를 제외하고 브로드캐스트한다")
  void handleRedisMessage_excludes_sender_for_relationship_extra_preview()
      throws Exception {
    ObjectNode extra = objectMapper.createObjectNode()
        .put("fkHandle", "right");
    String message = objectMapper.writeValueAsString(
        CollaborationOutboundFactory.relationshipExtraPreview("session-1",
            PreviewAction.UPDATE, "schema-1", "rel-1", extra));

    StepVerifier.create(
        collaborationService.handleRedisMessage("project-1", message))
        .verifyComplete();

    ArgumentCaptor<BroadcastMessage> captor = ArgumentCaptor.forClass(
        BroadcastMessage.class);
    verify(sessionRegistry).broadcast(captor.capture());
    assertThat(captor.getValue().excludeSessionId())
        .isEqualTo("session-1");
    assertThat(captor.getValue().message())
        .contains("\"type\":\"RELATIONSHIP_EXTRA_PREVIEW\"");
    assertThat(captor.getValue().message())
        .contains("\"action\":\"UPDATE\"");
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
    assertThat(captor.getValue().message())
        .contains("\"sessionId\":\"session-1\"");
  }

  @Test
  @DisplayName("SESSION_READY 이벤트는 Redis 브로드캐스트를 무시한다")
  void handleRedisMessage_ignores_session_ready_event() throws Exception {
    String message = objectMapper.writeValueAsString(
        CollaborationOutboundFactory.sessionReady("session-1",
            List.of()));

    StepVerifier.create(
        collaborationService.handleRedisMessage("project-1", message))
        .verifyComplete();

    verify(sessionRegistry, never()).broadcast(any());
  }

  @Test
  @DisplayName("세션 등록 시 Redis presence 등록 후 현재 참가자 snapshot을 반환한다")
  void registerSession_returns_presence_snapshot() {
    ProjectPresenceSession current = new ProjectPresenceSession(
        "session-1", "user-1", "tester", 1000L, 1000L);
    ProjectPresenceSession other = new ProjectPresenceSession(
        "session-2", "user-2", "other", 900L, 950L);
    given(presenceStore.register("project-1", "session-1", "user-1",
        "tester"))
        .willReturn(Mono.just(current));
    given(presenceStore.removeExpired("project-1")).willReturn(Flux.empty());
    given(presenceStore.findParticipants("project-1"))
        .willReturn(Flux.just(other, current));

    StepVerifier.create(collaborationService.registerSession("project-1",
        "session-1", "user-1", "tester"))
        .assertNext(participants -> {
          assertThat(participants)
              .extracting(ProjectPresenceParticipant::sessionId)
              .containsExactly("session-2", "session-1");
          assertThat(participants)
              .allSatisfy(participant -> assertThat(
                  participant.profileImageUrl()).isNull());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("presence refresh 시 Redis 세션이 없으면 로컬 세션으로 재등록하고 JOIN을 발행한다")
  void refreshPresence_reregisters_missing_presence_and_publishes_join() {
    SessionEntry entry = mock(SessionEntry.class);
    ProjectPresenceSession restored = new ProjectPresenceSession(
        "session-1", "user-1", "tester", 1000L, 1000L);
    given(presenceStore.refresh("project-1", "session-1"))
        .willReturn(Mono.empty());
    given(sessionRegistry.getSessionEntry("project-1", "session-1"))
        .willReturn(Optional.of(entry));
    given(entry.authInfo()).willReturn(WebSocketAuthInfo.of("user-1",
        "tester"));
    given(presenceStore.register("project-1", "session-1", "user-1",
        "tester"))
        .willReturn(Mono.just(restored));
    given(eventPublisher.publish(eq("project-1"), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(collaborationService.refreshPresence("project-1",
        "session-1"))
        .verifyComplete();

    verify(eventPublisher).publish(eq("project-1"),
        argThat(event -> event.type().name().equals("JOIN")
            && event.sessionId().equals("session-1")));
  }

  @Test
  @DisplayName("만료된 presence 세션은 LEAVE 이벤트로 발행한다")
  void removeExpiredPresenceSessions_publishes_leave_events() {
    ProjectPresenceSession expired = new ProjectPresenceSession(
        "session-2", "user-2", "other", 900L, 950L);
    given(presenceStore.findActiveProjectIds())
        .willReturn(Flux.just("project-1"));
    given(presenceStore.removeExpired("project-1"))
        .willReturn(Flux.just(expired));
    given(eventPublisher.publish(eq("project-1"), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(collaborationService.removeExpiredPresenceSessions())
        .verifyComplete();

    verify(eventPublisher, times(1)).publish(
        eq("project-1"),
        argThat(event -> event.sessionId().equals("session-2")));
  }

}
