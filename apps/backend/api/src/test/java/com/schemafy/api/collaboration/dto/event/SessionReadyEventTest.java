package com.schemafy.api.collaboration.dto.event;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.ProjectPresenceParticipant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionReadyEvent 직렬화 테스트")
class SessionReadyEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("Outbound 직렬화 시 type, sessionId, 참가자 snapshot이 포함된다")
  void serialize_includes_type_session_id_and_participants() throws Exception {
    SessionReadyEvent.Outbound event = SessionReadyEvent.Outbound.of(
        "session-1", List.of(new ProjectPresenceParticipant(
            "session-1", "user-1", "tester", null)));

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"type\":\"SESSION_READY\"");
    assertThat(json).contains("\"sessionId\":\"session-1\"");
    assertThat(json).doesNotContain("participantCount");
    assertThat(json).contains("\"participants\"");
    assertThat(json).contains("\"userName\":\"tester\"");
    assertThat(json).contains("\"profileImageUrl\":null");
    assertThat(json).doesNotContain("joinedAt");
    assertThat(json).doesNotContain("lastSeenAt");
  }

  @Test
  @DisplayName("Outbound 역직렬화 시 올바른 타입으로 복원된다")
  void deserialize_restores_correct_type() throws Exception {
    SessionReadyEvent.Outbound original = SessionReadyEvent.Outbound.of(
        "session-1", List.of());

    String json = objectMapper.writeValueAsString(original);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(deserialized).isInstanceOf(SessionReadyEvent.Outbound.class);
    SessionReadyEvent.Outbound event = (SessionReadyEvent.Outbound) deserialized;
    assertThat(event.type()).isEqualTo(CollaborationEventType.SESSION_READY);
    assertThat(event.sessionId()).isEqualTo("session-1");
    assertThat(event.participants()).isEmpty();
    assertThat(event.timestamp()).isGreaterThan(0);
  }

}
