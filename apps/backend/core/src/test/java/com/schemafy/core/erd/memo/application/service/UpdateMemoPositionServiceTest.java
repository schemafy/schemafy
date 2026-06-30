package com.schemafy.core.erd.memo.application.service;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonMetadataCanonicalizer;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoPositionCommand;
import com.schemafy.core.erd.memo.application.port.out.ChangeMemoPositionPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMemoPositionService")
class UpdateMemoPositionServiceTest {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  @Mock
  GetMemoByIdPort getMemoByIdPort;

  @Mock
  ChangeMemoPositionPort changeMemoPositionPort;

  @Spy
  JsonMetadataCanonicalizer jsonMetadataCanonicalizer = new JsonMetadataCanonicalizer(
      new JsonCodec(objectMapper));

  @InjectMocks
  UpdateMemoPositionService sut;

  @Test
  @DisplayName("작성자 본인은 위치를 수정할 수 있다")
  void update_success() {
    Memo original = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);
    Memo expectedUpdated = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{\"x\":100}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:01Z"),
        null);

    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(original), Mono.just(expectedUpdated));
    given(changeMemoPositionPort.changeMemoPosition("memo-1", "{\"x\":100}"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.updateMemoPosition(new UpdateMemoPositionCommand(
        "memo-1",
        jsonObject("{\"x\": 100}"),
        "author-1")))
        .assertNext(updated -> {
          assertThat(updated.id()).isEqualTo("memo-1");
          assertThat(updated.positions()).isEqualTo("{\"x\":100}");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("작성자가 아니면 ACCESS_DENIED를 반환한다")
  void update_accessDenied() {
    Memo memo = new Memo(
        "memo-1",
        "schema-1",
        "author-1",
        "{}",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        null);

    given(getMemoByIdPort.findMemoById("memo-1"))
        .willReturn(Mono.just(memo));

    StepVerifier.create(sut.updateMemoPosition(new UpdateMemoPositionCommand(
        "memo-1",
        jsonObject("{\"x\": 100}"),
        "other-user")))
        .expectErrorMatches(DomainException.hasErrorCode(
            MemoErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("위치가 JSON object가 아니면 error signal로 반환한다")
  void returnsErrorSignalWhenPositionIsNotObject() {
    StepVerifier.create(sut.updateMemoPosition(new UpdateMemoPositionCommand(
        "memo-1",
        TextNode.valueOf("invalid"),
        "author-1")))
        .expectError(IllegalArgumentException.class)
        .verify();

    then(getMemoByIdPort).shouldHaveNoInteractions();
    then(changeMemoPositionPort).shouldHaveNoInteractions();
  }

  private static JsonNode jsonObject(String rawJson) {
    try {
      return objectMapper.readTree(rawJson);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid test JSON", e);
    }
  }

}
