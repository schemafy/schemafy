package com.schemafy.core.collaboration.dto.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.dto.CollaborationEventType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErdStateChangedEvent 직렬화 테스트")
class ErdStateChangedEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("ACTIVE complete state가 wire round-trip에서 보존된다")
  void active_roundTrip_preserves_complete_state() throws Exception {
    JsonNode schema = objectMapper.readTree("""
        {"id":"schema-1","projectId":"project-1","name":"service"}
        """);
    JsonNode snapshots = objectMapper.readTree("""
        {"table-1":{"table":{"id":"table-1"},"columns":[]}}
        """);
    ErdStateChangedEvent.Outbound original = ErdStateChangedEvent.Outbound
        .active("schema-1", 42L, schema, snapshots);

    String json = objectMapper.writeValueAsString(original);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(json).contains("\"type\":\"ERD_STATE_CHANGED\"");
    assertThat(json).doesNotContain("\"operation\"");
    assertThat(deserialized).isInstanceOf(ErdStateChangedEvent.Outbound.class);
    ErdStateChangedEvent.Outbound event = (ErdStateChangedEvent.Outbound) deserialized;
    assertThat(event.type()).isEqualTo(CollaborationEventType.ERD_STATE_CHANGED);
    assertThat(event.schemaId()).isEqualTo("schema-1");
    assertThat(event.revision()).isEqualTo(42L);
    assertThat(event.state()).isEqualTo(ErdStateChangedEvent.State.ACTIVE);
    assertThat(event.schema()).isEqualTo(schema);
    assertThat(event.snapshots()).isEqualTo(snapshots);
    assertThat(event.sessionId()).isNull();
  }

  @Test
  @DisplayName("DELETED tombstone은 null state payload와 delete revision을 전달한다")
  void deleted_serializes_explicit_null_state_payload() throws Exception {
    ErdStateChangedEvent.Outbound event = ErdStateChangedEvent.Outbound
        .deleted("schema-1", 43L);

    String json = objectMapper.writeValueAsString(event);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(json).contains("\"state\":\"DELETED\"");
    assertThat(json).contains("\"schema\":null");
    assertThat(json).contains("\"snapshots\":null");
    assertThat(json).doesNotContain("\"operation\"");
    ErdStateChangedEvent.Outbound tombstone = (ErdStateChangedEvent.Outbound) deserialized;
    assertThat(tombstone.revision()).isEqualTo(43L);
    assertThat(tombstone.schema()).isNull();
    assertThat(tombstone.snapshots()).isNull();
  }

  @Test
  @DisplayName("ERD_STATE_CHANGED는 요청자를 포함해 broadcast한다")
  void eventType_includes_sender() {
    assertThat(CollaborationEventType.ERD_STATE_CHANGED.shouldIncludeSender())
        .isTrue();
    assertThat(CollaborationEventType.ERD_MUTATED.shouldIncludeSender())
        .isFalse();
  }

}
