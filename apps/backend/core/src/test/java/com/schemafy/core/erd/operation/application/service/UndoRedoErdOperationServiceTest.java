package com.schemafy.core.erd.operation.application.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UndoRedoErdOperationService")
class UndoRedoErdOperationServiceTest {

  private static final String PROJECT_ID = "project-1";
  private static final String SCHEMA_ID = "schema-1";

  @Mock
  UndoRedoEligibilityService undoRedoEligibilityService;

  @Mock
  UndoRedoErdOperationHandler handler;

  @Test
  @DisplayName("eligible한 undo 요청에 handler가 없으면 UNSUPPORTED를 반환한다")
  void returnsUnsupportedWhenNoUndoHandlerExists() {
    var resolved = resolved(UndoRedoAction.UNDO,
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationType.CHANGE_TABLE_NAME);
    UndoRedoErdOperationService sut = new UndoRedoErdOperationService(
        undoRedoEligibilityService,
        List.of());

    given(undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, "op-1"))
        .willReturn(Mono.just(resolved));

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .expectErrorMatches(DomainException.hasErrorCode(
            OperationErrorCode.UNSUPPORTED))
        .verify();
  }

  @Test
  @DisplayName("eligible한 redo 요청에 handler가 없으면 UNSUPPORTED를 반환한다")
  void returnsUnsupportedWhenNoRedoHandlerExists() {
    var resolved = resolved(UndoRedoAction.REDO,
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationType.CHANGE_TABLE_NAME);
    UndoRedoErdOperationService sut = new UndoRedoErdOperationService(
        undoRedoEligibilityService,
        List.of());

    given(undoRedoEligibilityService.resolve(UndoRedoAction.REDO, "op-1"))
        .willReturn(Mono.just(resolved));

    StepVerifier.create(sut.redo(new RedoErdOperationCommand("op-1")))
        .expectErrorMatches(DomainException.hasErrorCode(
            OperationErrorCode.UNSUPPORTED))
        .verify();
  }

  @ParameterizedTest
  @EnumSource(value = OperationErrorCode.class, names = {
      "NOT_FOUND",
      "SUPERSEDED",
      "ALREADY_UNDONE",
      "REDO_NOT_ELIGIBLE"
  })
  @DisplayName("eligibility 서비스의 DomainException은 그대로 전달한다")
  void propagatesEligibilityDomainExceptions(OperationErrorCode errorCode) {
    UndoRedoErdOperationService sut = new UndoRedoErdOperationService(
        undoRedoEligibilityService,
        List.of(handler));

    given(undoRedoEligibilityService.resolve(any(), eq("op-1")))
        .willReturn(Mono.error(new DomainException(errorCode)));

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .expectErrorMatches(DomainException.hasErrorCode(errorCode))
        .verify();
  }

  @Test
  @DisplayName("handler 조회는 target operation이 아니라 root original operation 타입을 기준으로 수행한다")
  void usesRootOriginalOperationTypeForHandlerLookup() {
    var resolved = resolved(UndoRedoAction.UNDO,
        ErdOperationType.CHANGE_COLUMN_NAME,
        ErdOperationType.CHANGE_TABLE_NAME);
    UndoRedoErdOperationService sut = new UndoRedoErdOperationService(
        undoRedoEligibilityService,
        List.of(handler));

    given(undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, "op-1"))
        .willReturn(Mono.just(resolved));
    given(handler.supports(ErdOperationType.CHANGE_TABLE_NAME))
        .willReturn(false);

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .expectErrorMatches(DomainException.hasErrorCode(
            OperationErrorCode.UNSUPPORTED))
        .verify();

    then(handler).should().supports(ErdOperationType.CHANGE_TABLE_NAME);
    then(handler).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("지원하는 handler가 있으면 undo를 delegation 한다")
  void delegatesUndoToSupportingHandler() {
    var resolved = resolved(UndoRedoAction.UNDO,
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationType.CHANGE_TABLE_NAME);
    var expected = MutationResult.<Void>of(null, Set.of("table-1"));
    UndoRedoErdOperationService sut = new UndoRedoErdOperationService(
        undoRedoEligibilityService,
        List.of(handler));

    given(undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, "op-1"))
        .willReturn(Mono.just(resolved));
    given(handler.supports(ErdOperationType.CHANGE_TABLE_NAME))
        .willReturn(true);
    given(handler.undo(resolved))
        .willReturn(Mono.just(expected));

    StepVerifier.create(sut.undo(new UndoErdOperationCommand("op-1")))
        .assertNext(result -> assertThat(result).isEqualTo(expected))
        .verifyComplete();

    then(handler).should().undo(resolved);
  }

  @Test
  @DisplayName("지원하는 handler가 있으면 redo를 delegation 한다")
  void delegatesRedoToSupportingHandler() {
    var resolved = resolved(UndoRedoAction.REDO,
        ErdOperationType.CHANGE_TABLE_NAME,
        ErdOperationType.CHANGE_TABLE_NAME);
    var expected = MutationResult.<Void>of(null, Set.of("table-1"));
    UndoRedoErdOperationService sut = new UndoRedoErdOperationService(
        undoRedoEligibilityService,
        List.of(handler));

    given(undoRedoEligibilityService.resolve(UndoRedoAction.REDO, "op-1"))
        .willReturn(Mono.just(resolved));
    given(handler.supports(ErdOperationType.CHANGE_TABLE_NAME))
        .willReturn(true);
    given(handler.redo(resolved))
        .willReturn(Mono.just(expected));

    StepVerifier.create(sut.redo(new RedoErdOperationCommand("op-1")))
        .assertNext(result -> assertThat(result).isEqualTo(expected))
        .verifyComplete();

    then(handler).should().redo(resolved);
  }

  private ResolvedUndoRedoEligibility resolved(
      UndoRedoAction action,
      ErdOperationType targetOpType,
      ErdOperationType rootOriginalOpType) {
    ErdOperationLog target = operation("target-op", targetOpType,
        ErdOperationDerivationKind.REDO, "root-op", 5);
    ErdOperationLog root = operation("root-op", rootOriginalOpType,
        ErdOperationDerivationKind.ORIGINAL, null, 3);
    ErdOperationLog chainTip = operation("chain-tip-op", rootOriginalOpType,
        action == UndoRedoAction.UNDO
            ? ErdOperationDerivationKind.REDO
            : ErdOperationDerivationKind.UNDO,
        root.opId(),
        6);
    return new ResolvedUndoRedoEligibility(
        action,
        target,
        root,
        chainTip,
        root,
        root,
        chainTip);
  }

  private ErdOperationLog operation(
      String opId,
      ErdOperationType opType,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId,
      long committedRevision) {
    return new ErdOperationLog(
        opId,
        PROJECT_ID,
        SCHEMA_ID,
        opType,
        committedRevision,
        committedRevision - 1,
        "client-op-" + opId,
        "session-1",
        "user-1",
        derivationKind,
        derivedFromOpId,
        ErdOperationLifecycleState.COMMITTED,
        "{}",
        null,
        "[]",
        "[]");
  }

}
