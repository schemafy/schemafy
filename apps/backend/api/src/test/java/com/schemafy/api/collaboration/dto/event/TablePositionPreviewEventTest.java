package com.schemafy.api.collaboration.dto.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.PreviewAction;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TablePositionPreviewEvent 직렬화 테스트")
class TablePositionPreviewEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("Outbound 직렬화 시 type, action, position을 포함한다")
  void serialize_includes_type_action_and_position() throws Exception {
    ObjectNode position = objectMapper.createObjectNode()
        .put("x", 120)
        .put("y", 80);
    TablePositionPreviewEvent.Outbound event = TablePositionPreviewEvent.Outbound.of(
        "session-1", PreviewAction.UPDATE, "schema-1", "table-1", position);

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"type\":\"TABLE_POSITION_PREVIEW\"");
    assertThat(json).contains("\"action\":\"UPDATE\"");
    assertThat(json).contains("\"sessionId\":\"session-1\"");
    assertThat(json).contains("\"schemaId\":\"schema-1\"");
    assertThat(json).contains("\"tableId\":\"table-1\"");
    assertThat(json).contains("\"position\":{\"x\":120,\"y\":80}");
  }

  @Test
  @DisplayName("Outbound 역직렬화 시 올바른 타입으로 복원된다")
  void deserialize_restores_correct_type() throws Exception {
    ObjectNode position = objectMapper.createObjectNode()
        .put("x", 12)
        .put("y", 34);
    TablePositionPreviewEvent.Outbound original = TablePositionPreviewEvent.Outbound.of(
        "session-1", PreviewAction.UPDATE, "schema-1", "table-1", position);

    String json = objectMapper.writeValueAsString(original);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(deserialized).isInstanceOf(TablePositionPreviewEvent.Outbound.class);
    TablePositionPreviewEvent.Outbound event = (TablePositionPreviewEvent.Outbound) deserialized;
    assertThat(event.type()).isEqualTo(CollaborationEventType.TABLE_POSITION_PREVIEW);
    assertThat(event.action()).isEqualTo(PreviewAction.UPDATE);
    assertThat(event.position()).isEqualTo(position);
  }

  @Test
  @DisplayName("CLEAR 이벤트는 position 없이 직렬화된다")
  void serialize_clear_omits_position() throws Exception {
    TablePositionPreviewEvent.Outbound event = TablePositionPreviewEvent.Outbound.of(
        "session-1", PreviewAction.CLEAR, "schema-1", "table-1", null);

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"action\":\"CLEAR\"");
    assertThat(json).doesNotContain("\"position\"");
  }

}
