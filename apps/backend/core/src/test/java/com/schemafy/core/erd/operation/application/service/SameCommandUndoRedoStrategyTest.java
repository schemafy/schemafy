package com.schemafy.core.erd.operation.application.service;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommandUndoRedoStrategy")
class SameCommandUndoRedoStrategyTest {

  @Mock
  SameCommandReplayRegistry sameCommandReplayRegistry;

  @Spy
  JsonCodec jsonCodec = new JsonCodec(new ObjectMapper());

  @InjectMocks
  SameCommandUndoRedoStrategy sut;

  @Test
  @DisplayName("undo는 inverse payload를 사용한다")
  void undoesFromInversePayload() {
    ErdOperationLog operationLog = operationLog(
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new")),
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old")));

    given(sameCommandReplayRegistry.executePersisted(
        ErdOperationType.CHANGE_INDEX_NAME,
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old"))))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.undo(operationLog))
        .assertNext(result -> assertThat(result.affectedTableIds()).containsExactly("table-1"))
        .verifyComplete();

    then(sameCommandReplayRegistry).should().executePersisted(
        ErdOperationType.CHANGE_INDEX_NAME,
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old")));
  }

  @Test
  @DisplayName("redo는 원래 payload를 사용한다")
  void redoesFromOriginalPayload() {
    ErdOperationLog operationLog = operationLog(
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new")),
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old")));

    given(sameCommandReplayRegistry.executePersisted(
        ErdOperationType.CHANGE_INDEX_NAME,
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new"))))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.redo(operationLog))
        .expectNextCount(1)
        .verifyComplete();

    then(sameCommandReplayRegistry).should().executePersisted(
        ErdOperationType.CHANGE_INDEX_NAME,
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new")));
  }

  @Test
  @DisplayName("same-command undo는 파생 metadata를 내부에서 주입한다")
  void seedsDerivedMetadataInsideUndo() {
    ErdOperationLog operationLog = operationLog(
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new")),
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old")));
    AtomicReference<ErdOperationMetadata> metadataRef = new AtomicReference<>();

    given(sameCommandReplayRegistry.executePersisted(
        ErdOperationType.CHANGE_INDEX_NAME,
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old"))))
        .willReturn(Mono.deferContextual(ctx -> {
          metadataRef.set(ErdOperationContexts.metadata(ctx));
          return Mono.just(MutationResult.of(null, "table-1"));
        }));

    StepVerifier.create(sut.undo(operationLog))
        .expectNextCount(1)
        .verifyComplete();

    assertThat(metadataRef.get()).isEqualTo(new ErdOperationMetadata(
        null,
        null,
        null,
        null,
        ErdOperationDerivationKind.UNDO,
        "op-1"));
  }

  @Test
  @DisplayName("undo payload가 없으면 예외가 발생한다")
  void throwsWhenInversePayloadMissing() {
    String payloadJson = jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new"));

    StepVerifier.create(sut.undo(operationLog(payloadJson, null)))
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(DomainException.class)
            .matches(DomainException.hasErrorCode(OperationErrorCode.UNSUPPORTED))
            .hasMessageContaining("Undo payload is missing"))
        .verify();
  }

  @Test
  @DisplayName("registry가 지원하지 않는 연산 예외를 반환하면 그대로 전달한다")
  void propagatesUnsupportedOperationError() {
    ErdOperationLog operationLog = operationLog(ErdOperationType.DELETE_TABLE, "{}", "{}");

    given(sameCommandReplayRegistry.executePersisted(ErdOperationType.DELETE_TABLE, "{}"))
        .willReturn(Mono.error(new IllegalArgumentException(
            "Unsupported same-command undo/redo operation: DELETE_TABLE")));

    StepVerifier.create(sut.redo(operationLog))
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported same-command undo/redo operation: DELETE_TABLE"))
        .verify();
  }

  private ErdOperationLog operationLog(String payloadJson, String inversePayloadJson) {
    return operationLog(ErdOperationType.CHANGE_INDEX_NAME, payloadJson, inversePayloadJson);
  }

  private ErdOperationLog operationLog(
      ErdOperationType opType,
      String payloadJson,
      String inversePayloadJson) {
    return new ErdOperationLog(
        "op-1",
        "project-1",
        "schema-1",
        opType,
        3L,
        null,
        null,
        null,
        "user-1",
        ErdOperationDerivationKind.ORIGINAL,
        null,
        ErdOperationLifecycleState.COMMITTED,
        payloadJson,
        inversePayloadJson,
        "[]",
        "[\"table-1\"]");
  }

}
