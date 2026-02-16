package com.schemafy.core.collaboration.dto.event;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErdMutatedEvent 직렬화 테스트")
class ErdMutatedEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("Outbound 직렬화 시 type 디스크리미네이터가 포함된다")
  void serialize_includes_type_discriminator() throws Exception {
    ErdMutatedEvent.Outbound event = ErdMutatedEvent.Outbound.of(
        "schema-1", Set.of("table-1", "table-2"));

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"type\":\"ERD_MUTATED\"");
    assertThat(json).contains("\"schemaId\":\"schema-1\"");
    assertThat(json).contains("\"table-1\"");
    assertThat(json).contains("\"table-2\"");
  }

  @Test
  @DisplayName("Outbound 역직렬화 시 올바른 타입으로 복원된다")
  void deserialize_restores_correct_type() throws Exception {
    ErdMutatedEvent.Outbound original = ErdMutatedEvent.Outbound.of(
        "schema-1", Set.of("table-1"));

    String json = objectMapper.writeValueAsString(original);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(deserialized).isInstanceOf(ErdMutatedEvent.Outbound.class);
    ErdMutatedEvent.Outbound event = (ErdMutatedEvent.Outbound) deserialized;
    assertThat(event.type()).isEqualTo(CollaborationEventType.ERD_MUTATED);
    assertThat(event.schemaId()).isEqualTo("schema-1");
    assertThat(event.affectedTableIds()).containsExactly("table-1");
    assertThat(event.sessionId()).isNull();
  }

  @Test
  @DisplayName("Outbound of 팩토리 메서드가 sessionId를 null로 설정한다")
  void of_sets_null_session_id() {
    ErdMutatedEvent.Outbound event = ErdMutatedEvent.Outbound.of(
        "schema-1", Set.of());

    assertThat(event.sessionId()).isNull();
    assertThat(event.timestamp()).isGreaterThan(0);
  }

  @Test
  @DisplayName("withoutSessionId는 sessionId가 null인 새 인스턴스를 반환한다")
  void withoutSessionId_returns_new_instance_without_session() {
    ErdMutatedEvent.Outbound event = ErdMutatedEvent.Outbound.of(
        "session-1", "schema-1", Set.of("table-1"));

    ErdMutatedEvent.Outbound stripped = (ErdMutatedEvent.Outbound) event
        .withoutSessionId();

    assertThat(stripped).isNotSameAs(event);
    assertThat(stripped.sessionId()).isNull();
    assertThat(stripped.schemaId()).isEqualTo("schema-1");
    assertThat(stripped.affectedTableIds()).containsExactly("table-1");
    assertThat(stripped.timestamp()).isEqualTo(event.timestamp());
  }

  @Test
  @DisplayName("sessionId가 있는 팩토리 메서드가 정상 동작한다")
  void of_with_session_id() {
    ErdMutatedEvent.Outbound event = ErdMutatedEvent.Outbound.of(
        "session-1", "schema-1", Set.of("table-1"));

    assertThat(event.sessionId()).isEqualTo("session-1");
    assertThat(event.schemaId()).isEqualTo("schema-1");
  }

}
