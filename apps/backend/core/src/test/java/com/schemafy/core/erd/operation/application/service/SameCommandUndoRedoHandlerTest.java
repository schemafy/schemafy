package com.schemafy.core.erd.operation.application.service;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommandUndoRedoHandler")
class SameCommandUndoRedoHandlerTest {

  @Mock
  SameCommandUndoRedoStrategy strategy;

  @InjectMocks
  SameCommandUndoRedoHandler sut;

  @Test
  @DisplayName("supports는 strategy 지원 여부를 그대로 위임한다")
  void delegatesSupportsToStrategy() {
    given(strategy.supports(ErdOperationType.CHANGE_TABLE_NAME))
        .willReturn(true);

    assertThat(sut.supports(ErdOperationType.CHANGE_TABLE_NAME)).isTrue();

    then(strategy).should().supports(ErdOperationType.CHANGE_TABLE_NAME);
  }

  @Test
  @DisplayName("undo는 execution base operation으로 strategy를 호출한다")
  void delegatesUndoWithExecutionBaseOperation() {
    ErdOperationLog root = operation(
        "root-op",
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationDerivationKind.ORIGINAL,
        null);
    ErdOperationLog chainTip = operation(
        "chain-tip-op",
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationDerivationKind.REDO,
        root.opId());
    ResolvedUndoRedoEligibility resolved = new ResolvedUndoRedoEligibility(
        UndoRedoAction.UNDO,
        chainTip,
        root,
        chainTip,
        root,
        root,
        chainTip);
    MutationResult<Void> expected = MutationResult.of(null, Set.of("table-1"));

    given(strategy.undo(chainTip))
        .willReturn(Mono.just(expected));

    StepVerifier.create(sut.undo(resolved))
        .assertNext(result -> assertThat(result).isEqualTo(expected))
        .verifyComplete();

    then(strategy).should().undo(chainTip);
  }

  @Test
  @DisplayName("redo는 execution base operation으로 strategy를 호출한다")
  void delegatesRedoWithExecutionBaseOperation() {
    ErdOperationLog target = operation(
        "target-op",
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationDerivationKind.UNDO,
        "root-op");
    ErdOperationLog root = operation(
        "root-op",
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationDerivationKind.ORIGINAL,
        null);
    ErdOperationLog chainTip = operation(
        "chain-tip-op",
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationDerivationKind.UNDO,
        root.opId());
    ResolvedUndoRedoEligibility resolved = new ResolvedUndoRedoEligibility(
        UndoRedoAction.REDO,
        target,
        root,
        chainTip,
        root,
        root,
        chainTip);
    MutationResult<Void> expected = MutationResult.of(null, Set.of("table-1"));

    given(strategy.redo(root))
        .willReturn(Mono.just(expected));

    StepVerifier.create(sut.redo(resolved))
        .assertNext(result -> assertThat(result).isEqualTo(expected))
        .verifyComplete();

    then(strategy).should().redo(root);
  }

  private ErdOperationLog operation(
      String opId,
      ErdOperationType opType,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId) {
    return new ErdOperationLog(
        opId,
        "project-1",
        "schema-1",
        opType,
        3L,
        2L,
        "client-op-" + opId,
        "session-1",
        "user-1",
        derivationKind,
        derivedFromOpId,
        ErdOperationLifecycleState.COMMITTED,
        "{}",
        "{}",
        "[]",
        "[]");
  }

}
