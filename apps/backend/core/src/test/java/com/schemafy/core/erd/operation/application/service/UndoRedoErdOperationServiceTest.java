package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.out.FindErdOperationLogPort;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UndoRedoErdOperationService")
class UndoRedoErdOperationServiceTest {

  @Mock
  FindErdOperationLogPort findErdOperationLogPort;

  @Mock
  UndoRedoErdOperationStrategy firstStrategy;

  @Mock
  UndoRedoErdOperationStrategy secondStrategy;

  UndoRedoErdOperationService sut;

  @BeforeEach
  void setUp() {
    sut = new UndoRedoErdOperationService(
        findErdOperationLogPort,
        List.of(firstStrategy, secondStrategy));
  }

  @Test
  @DisplayName("undo는 opId가 비어 있으면 예외가 발생한다")
  void throwsWhenUndoOpIdBlank() {
    StepVerifier.create(sut.undo(new UndoErdOperationCommand(" ")))
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("opId must not be blank"))
        .verify();
  }

  @Test
  @DisplayName("operation이 없으면 예외가 발생한다")
  void throwsWhenOperationMissing() {
    given(findErdOperationLogPort.findByOpId("op-1"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation not found: op-1"))
        .verify();
  }

  @Test
  @DisplayName("undo는 지원하는 strategy에 위임한다")
  void delegatesUndoToSupportingStrategy() {
    ErdOperationLog operationLog = operationLog(ErdOperationType.CHANGE_INDEX_NAME);

    given(findErdOperationLogPort.findByOpId("op-1"))
        .willReturn(Mono.just(operationLog));
    given(firstStrategy.supports(ErdOperationType.CHANGE_INDEX_NAME))
        .willReturn(false);
    given(secondStrategy.supports(ErdOperationType.CHANGE_INDEX_NAME))
        .willReturn(true);
    given(secondStrategy.undo(operationLog))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .assertNext(result -> assertThat(result.affectedTableIds()).containsExactly("table-1"))
        .verifyComplete();

    then(secondStrategy).should().undo(operationLog);
    then(secondStrategy).should(never()).redo(operationLog);
  }

  @Test
  @DisplayName("redo는 지원하는 strategy에 위임한다")
  void delegatesRedoToSupportingStrategy() {
    ErdOperationLog operationLog = operationLog(ErdOperationType.CHANGE_INDEX_NAME);

    given(findErdOperationLogPort.findByOpId("op-1"))
        .willReturn(Mono.just(operationLog));
    given(firstStrategy.supports(ErdOperationType.CHANGE_INDEX_NAME))
        .willReturn(true);
    given(firstStrategy.redo(operationLog))
        .willReturn(Mono.just(MutationResult.of(null, "table-1")));

    StepVerifier.create(sut.redo(new RedoErdOperationCommand("op-1")))
        .expectNextCount(1)
        .verifyComplete();

    then(firstStrategy).should().redo(operationLog);
    then(firstStrategy).should(never()).undo(operationLog);
  }

  @Test
  @DisplayName("지원하는 strategy가 없으면 예외가 발생한다")
  void throwsWhenNoStrategySupportsOperation() {
    ErdOperationLog operationLog = operationLog(ErdOperationType.DELETE_TABLE);

    given(findErdOperationLogPort.findByOpId("op-1"))
        .willReturn(Mono.just(operationLog));
    given(firstStrategy.supports(ErdOperationType.DELETE_TABLE))
        .willReturn(false);
    given(secondStrategy.supports(ErdOperationType.DELETE_TABLE))
        .willReturn(false);

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .expectErrorSatisfies(error -> assertThat(error)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported undo/redo operation: DELETE_TABLE"))
        .verify();
  }

  private ErdOperationLog operationLog(ErdOperationType opType) {
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
        "{}",
        "{}",
        "[]",
        "[\"table-1\"]");
  }

}
