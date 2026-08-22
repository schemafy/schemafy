package com.schemafy.api.collaboration.service;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.core.collaboration.dto.CursorPosition;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.dto.event.CursorEvent;
import com.schemafy.core.collaboration.dto.event.ErdMutatedEvent;
import com.schemafy.core.collaboration.dto.event.ErdStateChangedEvent;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollaborationPayloadSerializer 테스트")
class CollaborationPayloadSerializerTest {

  private static final CommittedErdOperation OPERATION = new CommittedErdOperation(
      "op-1",
      "client-op-1",
      42L,
      ErdOperationDerivationKind.ORIGINAL);

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();
  private final CollaborationPayloadSerializer serializer = new CollaborationPayloadSerializer(
      new JsonCodec(objectMapper));

  @Test
  @DisplayName("JOIN 이벤트 직렬화 시 sessionId를 포함한다")
  void serialize_includes_session_id_for_join() {
    StepVerifier.create(serializer.serialize(
        CollaborationOutboundFactory.join("session-1", "user-1",
            "tester")))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"JOIN\"");
          assertThat(json).contains("\"sessionId\":\"session-1\"");
          assertThat(json).contains("\"userId\":\"user-1\"");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("CURSOR 이벤트 직렬화 시 sessionId를 포함한다")
  void serialize_includes_session_id_for_cursor() {
    CursorEvent.UserInfo userInfo = new CursorEvent.UserInfo("user-1",
        "tester");

    StepVerifier.create(serializer.serialize(
        CollaborationOutboundFactory.cursor("session-1", userInfo,
            new CursorPosition(10.0, 20.0))))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"CURSOR\"");
          assertThat(json).contains("\"sessionId\":\"session-1\"");
          assertThat(json).contains("\"userInfo\"");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("ERD_MUTATED 이벤트 직렬화 시 sessionId를 포함한다")
  void serialize_includes_session_id_for_erd_mutated() {
    StepVerifier.create(serializer.serialize(
        CollaborationOutboundFactory.erdMutated("session-1", "schema-1",
            Set.of("table-1"), OPERATION)))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"ERD_MUTATED\"");
          assertThat(json).contains("\"sessionId\":\"session-1\"");
          assertThat(json).contains("\"schemaId\":\"schema-1\"");
          assertThat(json).contains("\"operation\":{");
          assertThat(json).contains("\"opId\":\"op-1\"");
          assertThat(json).contains("\"committedRevision\":42");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("sessionId가 없는 ERD_MUTATED 직렬화 시 sessionId를 생략한다")
  void serialize_omits_session_id_when_erd_mutated_has_no_session() {
    StepVerifier.create(serializer.serialize(
        ErdMutatedEvent.Outbound.of(null, "schema-1",
            Set.of("table-1"), OPERATION)))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"ERD_MUTATED\"");
          assertThat(json).contains("\"schemaId\":\"schema-1\"");
          assertThat(json).contains("\"committedRevision\":42");
          assertThat(json).doesNotContain("sessionId");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("ACTIVE ERD_STATE_CHANGED는 전체 상태와 revision만 직렬화한다")
  void serialize_active_erd_state_changed() {
    ObjectNode schema = objectMapper.createObjectNode()
        .put("id", "schema-1");
    ObjectNode snapshots = objectMapper.createObjectNode()
        .set("table-1", objectMapper.createObjectNode());

    StepVerifier.create(serializer.serialize(
        CollaborationOutboundFactory.erdStateChangedActive("schema-1", 42L,
            schema, snapshots)))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"ERD_STATE_CHANGED\"");
          assertThat(json).contains("\"schemaId\":\"schema-1\"");
          assertThat(json).contains("\"revision\":42");
          assertThat(json).contains("\"state\":\"ACTIVE\"");
          assertThat(json).contains("\"schema\":{\"id\":\"schema-1\"}");
          assertThat(json).contains("\"snapshots\":{\"table-1\":{}}");
          assertThat(json).doesNotContain("sessionId");
          assertThat(json).doesNotContain("operation");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("DELETED ERD_STATE_CHANGED는 null tombstone을 직렬화한다")
  void serialize_deleted_erd_state_changed() {
    StepVerifier.create(serializer.serialize(
        ErdStateChangedEvent.Outbound.deleted("schema-1", 43L)))
        .assertNext(json -> {
          assertThat(json).contains("\"state\":\"DELETED\"");
          assertThat(json).contains("\"revision\":43");
          assertThat(json).contains("\"schema\":null");
          assertThat(json).contains("\"snapshots\":null");
          assertThat(json).doesNotContain("operation");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("null 이벤트 직렬화 시 일관된 런타임 예외를 반환한다")
  void serialize_nullEvent_returnsWrappedRuntimeException() {
    StepVerifier.create(serializer.serialize(null))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(RuntimeException.class);
          assertThat(error.getMessage()).contains("failed to serialize JSON");
          assertThat(error.getCause()).isInstanceOf(IllegalArgumentException.class);
        })
        .verify();
  }

}
