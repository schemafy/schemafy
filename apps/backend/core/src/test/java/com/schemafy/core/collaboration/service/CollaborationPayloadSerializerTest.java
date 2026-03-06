package com.schemafy.core.collaboration.service;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.dto.CursorPosition;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.dto.event.CursorEvent;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollaborationPayloadSerializer 테스트")
class CollaborationPayloadSerializerTest {

  private final CollaborationPayloadSerializer serializer = new CollaborationPayloadSerializer(
      new ObjectMapper().findAndRegisterModules());

  @Test
  @DisplayName("JOIN 브로드캐스트 직렬화 시 sessionId를 제거한다")
  void serializeForBroadcast_removes_session_id_from_join() {
    StepVerifier.create(serializer.serializeForBroadcast(
        CollaborationOutboundFactory.join("session-1", "user-1",
            "tester")))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"JOIN\"");
          assertThat(json).contains("\"userId\":\"user-1\"");
          assertThat(json).doesNotContain("sessionId");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("CURSOR 브로드캐스트 직렬화 시 sessionId를 제거한다")
  void serializeForBroadcast_removes_session_id_from_cursor() {
    CursorEvent.UserInfo userInfo = new CursorEvent.UserInfo("user-1",
        "tester");

    StepVerifier.create(serializer.serializeForBroadcast(
        CollaborationOutboundFactory.cursor("session-1", userInfo,
            new CursorPosition(10.0, 20.0))))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"CURSOR\"");
          assertThat(json).contains("\"userInfo\"");
          assertThat(json).doesNotContain("sessionId");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("ERD_MUTATED 브로드캐스트 직렬화 시 sessionId를 제거한다")
  void serializeForBroadcast_removes_session_id_from_erd_mutated() {
    StepVerifier.create(serializer.serializeForBroadcast(
        CollaborationOutboundFactory.erdMutated("session-1", "schema-1",
            Set.of("table-1"))))
        .assertNext(json -> {
          assertThat(json).contains("\"type\":\"ERD_MUTATED\"");
          assertThat(json).contains("\"schemaId\":\"schema-1\"");
          assertThat(json).doesNotContain("sessionId");
        })
        .verifyComplete();
  }

}
