package com.schemafy.api.collaboration.dto.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.collaboration.dto.CollaborationEventType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionReadyEvent 직렬화 테스트")
class SessionReadyEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("Outbound 직렬화 시 type과 sessionId가 포함된다")
  void serialize_includes_type_and_session_id() throws Exception {
    SessionReadyEvent.Outbound event = SessionReadyEvent.Outbound.of(
        "session-1");

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"type\":\"SESSION_READY\"");
    assertThat(json).contains("\"sessionId\":\"session-1\"");
  }

  @Test
  @DisplayName("Outbound 역직렬화 시 올바른 타입으로 복원된다")
  void deserialize_restores_correct_type() throws Exception {
    SessionReadyEvent.Outbound original = SessionReadyEvent.Outbound.of(
        "session-1");

    String json = objectMapper.writeValueAsString(original);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(deserialized).isInstanceOf(SessionReadyEvent.Outbound.class);
    SessionReadyEvent.Outbound event = (SessionReadyEvent.Outbound) deserialized;
    assertThat(event.type()).isEqualTo(CollaborationEventType.SESSION_READY);
    assertThat(event.sessionId()).isEqualTo("session-1");
    assertThat(event.timestamp()).isGreaterThan(0);
  }

}
