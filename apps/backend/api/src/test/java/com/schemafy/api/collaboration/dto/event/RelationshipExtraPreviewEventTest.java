package com.schemafy.api.collaboration.dto.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.PreviewAction;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RelationshipExtraPreviewEvent 직렬화 테스트")
class RelationshipExtraPreviewEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Test
  @DisplayName("Outbound 직렬화 시 type, action, extra를 포함한다")
  void serialize_includes_type_action_and_extra() throws Exception {
    ObjectNode controlPoint1 = objectMapper.createObjectNode()
        .put("x", 220)
        .put("y", 100);
    ObjectNode extra = objectMapper.createObjectNode()
        .put("fkHandle", "right")
        .put("pkHandle", "left")
        .set("controlPoint1", controlPoint1);
    RelationshipExtraPreviewEvent.Outbound event = RelationshipExtraPreviewEvent.Outbound.of(
        "session-1", PreviewAction.UPDATE, "schema-1", "rel-1", extra);

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"type\":\"RELATIONSHIP_EXTRA_PREVIEW\"");
    assertThat(json).contains("\"action\":\"UPDATE\"");
    assertThat(json).contains("\"sessionId\":\"session-1\"");
    assertThat(json).contains("\"schemaId\":\"schema-1\"");
    assertThat(json).contains("\"relationshipId\":\"rel-1\"");
    assertThat(json).contains("\"fkHandle\":\"right\"");
    assertThat(json).contains("\"controlPoint1\":{\"x\":220,\"y\":100}");
  }

  @Test
  @DisplayName("Outbound 역직렬화 시 올바른 타입으로 복원된다")
  void deserialize_restores_correct_type() throws Exception {
    ObjectNode extra = objectMapper.createObjectNode()
        .put("fkHandle", "right")
        .put("pkHandle", "left");
    RelationshipExtraPreviewEvent.Outbound original = RelationshipExtraPreviewEvent.Outbound.of(
        "session-1", PreviewAction.UPDATE, "schema-1", "rel-1", extra);

    String json = objectMapper.writeValueAsString(original);
    CollaborationOutbound deserialized = objectMapper.readValue(json,
        CollaborationOutbound.class);

    assertThat(deserialized).isInstanceOf(RelationshipExtraPreviewEvent.Outbound.class);
    RelationshipExtraPreviewEvent.Outbound event = (RelationshipExtraPreviewEvent.Outbound) deserialized;
    assertThat(event.type()).isEqualTo(CollaborationEventType.RELATIONSHIP_EXTRA_PREVIEW);
    assertThat(event.action()).isEqualTo(PreviewAction.UPDATE);
    assertThat(event.extra()).isEqualTo(extra);
  }

  @Test
  @DisplayName("CLEAR 이벤트는 extra 없이 직렬화된다")
  void serialize_clear_omits_extra() throws Exception {
    RelationshipExtraPreviewEvent.Outbound event = RelationshipExtraPreviewEvent.Outbound.of(
        "session-1", PreviewAction.CLEAR, "schema-1", "rel-1", null);

    String json = objectMapper.writeValueAsString(event);

    assertThat(json).contains("\"action\":\"CLEAR\"");
    assertThat(json).doesNotContain("\"extra\"");
  }

}
