package com.schemafy.core.erd.operation.application.service;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommandUndoRedoStrategy")
class SameCommandUndoRedoStrategyTest {

  @Mock
  ChangeTableNameUseCase changeTableNameUseCase;

  @Mock
  ChangeColumnNameUseCase changeColumnNameUseCase;

  @Mock
  ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @Mock
  ChangeRelationshipNameUseCase changeRelationshipNameUseCase;

  @Mock
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @Mock
  ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;

  @Mock
  ChangeConstraintNameUseCase changeConstraintNameUseCase;

  @Mock
  ChangeIndexNameUseCase changeIndexNameUseCase;

  @Mock
  ChangeIndexTypeUseCase changeIndexTypeUseCase;

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

    given(changeIndexNameUseCase.changeIndexName(new ChangeIndexNameCommand("index-1", "idx_old")))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.undo(operationLog))
        .assertNext(result -> assertThat(result.affectedTableIds()).containsExactly("table-1"))
        .verifyComplete();

    then(changeIndexNameUseCase).should().changeIndexName(new ChangeIndexNameCommand("index-1", "idx_old"));
  }

  @Test
  @DisplayName("redo는 원래 payload를 사용한다")
  void redoesFromOriginalPayload() {
    ErdOperationLog operationLog = operationLog(
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new")),
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old")));

    given(changeIndexNameUseCase.changeIndexName(new ChangeIndexNameCommand("index-1", "idx_new")))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.redo(operationLog))
        .expectNextCount(1)
        .verifyComplete();

    then(changeIndexNameUseCase).should().changeIndexName(new ChangeIndexNameCommand("index-1", "idx_new"));
  }

  @Test
  @DisplayName("same-command undo는 파생 metadata를 내부에서 주입한다")
  void seedsDerivedMetadataInsideUndo() {
    ErdOperationLog operationLog = operationLog(
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_new")),
        jsonCodec.serialize(new ChangeIndexNameCommand("index-1", "idx_old")));
    AtomicReference<ErdOperationMetadata> metadataRef = new AtomicReference<>();

    given(changeIndexNameUseCase.changeIndexName(new ChangeIndexNameCommand("index-1", "idx_old")))
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
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Undo payload is missing"))
        .verify();
  }

  private ErdOperationLog operationLog(String payloadJson, String inversePayloadJson) {
    return new ErdOperationLog(
        "op-1",
        "project-1",
        "schema-1",
        ErdOperationType.CHANGE_INDEX_NAME,
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
