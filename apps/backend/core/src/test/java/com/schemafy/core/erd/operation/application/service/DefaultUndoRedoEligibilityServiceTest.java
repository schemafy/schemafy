package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.GetErdOperationByIdPort;
import com.schemafy.core.erd.operation.application.port.out.GetErdOperationsBySchemaIdPort;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultUndoRedoEligibilityService")
class DefaultUndoRedoEligibilityServiceTest {

  private static final String PROJECT_ID = "project-1";
  private static final String SCHEMA_ID = "schema-1";

  @Mock
  GetErdOperationByIdPort getErdOperationByIdPort;

  @Mock
  GetErdOperationsBySchemaIdPort getErdOperationsBySchemaIdPort;

  @InjectMocks
  DefaultUndoRedoEligibilityService sut;

  @Nested
  @DisplayName("resolve 메서드는")
  class Resolve {

    @Test
    @DisplayName("최신 original operation을 undo candidate로 해석한다")
    void resolvesLatestOriginalAsUndoCandidate() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      stubHistory(o3, List.of(o1, o2, o3));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, o3.opId()))
          .assertNext(result -> {
            assertThat(result.targetRootOriginalOperation().opId()).isEqualTo("op-3");
            assertThat(result.currentChainTipOperation().opId()).isEqualTo("op-3");
            assertThat(result.currentUndoCandidateOperation().opId()).isEqualTo("op-3");
            assertThat(result.currentRedoCandidateOperation()).isNull();
            assertThat(result.executionBaseOperation().opId()).isEqualTo("op-3");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("undo 이후에는 직전 original operation이 다음 undo candidate가 된다")
    void resolvesPreviousOriginalAfterUndo() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      var u3 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o3.opId());
      stubHistory(o2, List.of(o1, o2, o3, u3));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, o2.opId()))
          .assertNext(result -> {
            assertThat(result.targetRootOriginalOperation().opId()).isEqualTo("op-2");
            assertThat(result.currentChainTipOperation().opId()).isEqualTo("op-2");
            assertThat(result.currentUndoCandidateOperation().opId()).isEqualTo("op-2");
            assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo("op-3");
            assertThat(result.schemaHeadOperation().opId()).isEqualTo("op-4");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("현재 undo candidate가 아닌 더 오래된 applied operation은 SUPERSEDED를 반환한다")
    void returnsSupersededForOlderAppliedOperation() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      stubHistory(o1, List.of(o1, o2, o3));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, o1.opId()))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.SUPERSEDED))
          .verify();
    }

    @Test
    @DisplayName("이미 undone 된 operation을 다시 undo하면 ALREADY_UNDONE을 반환한다")
    void returnsAlreadyUndoneWhenUndoingUndoneOperation() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      var u3 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o3.opId());
      stubHistory(o3, List.of(o1, o2, o3, u3));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, o3.opId()))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.ALREADY_UNDONE))
          .verify();
    }

    @Test
    @DisplayName("최신 undo frontier를 redo candidate로 해석한다")
    void resolvesRedoCandidate() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      var u3 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o3.opId());
      stubHistory(o3, List.of(o1, o2, o3, u3));

      StepVerifier.create(sut.resolve(UndoRedoAction.REDO, o3.opId()))
          .assertNext(result -> {
            assertThat(result.targetRootOriginalOperation().opId()).isEqualTo("op-3");
            assertThat(result.currentChainTipOperation().opId()).isEqualTo("op-4");
            assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo("op-3");
            assertThat(result.executionBaseOperation().opId()).isEqualTo("op-3");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("undo -> undo -> redo 이후 파생 opId를 root original로 정규화한다")
    void normalizesDerivedTargetToOriginal() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      var u3 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o3.opId());
      var u2 = derived("op-5", 5, ErdOperationDerivationKind.UNDO, o2.opId());
      var r2 = derived("op-6", 6, ErdOperationDerivationKind.REDO, u2.opId());
      stubHistory(r2, List.of(o1, o2, o3, u3, u2, r2));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, r2.opId()))
          .assertNext(result -> {
            assertThat(result.targetOperation().opId()).isEqualTo("op-6");
            assertThat(result.targetRootOriginalOperation().opId()).isEqualTo("op-2");
            assertThat(result.currentChainTipOperation().opId()).isEqualTo("op-6");
            assertThat(result.currentUndoCandidateOperation().opId()).isEqualTo("op-2");
            assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo("op-3");
            assertThat(result.executionBaseOperation().opId()).isEqualTo("op-6");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("redo frontier가 새 original mutation으로 지워지면 REDO_NOT_ELIGIBLE을 반환한다")
    void returnsRedoNotEligibleWhenRedoFrontierIsCleared() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var o3 = original("op-3", 3);
      var u3 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o3.opId());
      var o4 = original("op-5", 5);
      stubHistory(o3, List.of(o1, o2, o3, u3, o4));

      StepVerifier.create(sut.resolve(UndoRedoAction.REDO, o3.opId()))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.REDO_NOT_ELIGIBLE))
          .verify();
    }

    @Test
    @DisplayName("여러 번 undo한 뒤에는 redo stack의 최상단 original operation만 redo할 수 있다")
    void returnsRedoNotEligibleForDeeperRedoHistory() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var u2 = derived("op-3", 3, ErdOperationDerivationKind.UNDO, o2.opId());
      var u1 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o1.opId());
      stubHistory(o2, List.of(o1, o2, u2, u1));

      StepVerifier.create(sut.resolve(UndoRedoAction.REDO, o2.opId()))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.REDO_NOT_ELIGIBLE))
          .verify();
    }

    @Test
    @DisplayName("현재 redo candidate를 복원하면 다음 redo candidate가 이어서 활성화된다")
    void resolvesNextRedoCandidateAfterRedoingTopOfRedoStack() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var u2 = derived("op-3", 3, ErdOperationDerivationKind.UNDO, o2.opId());
      var u1 = derived("op-4", 4, ErdOperationDerivationKind.UNDO, o1.opId());
      var r1 = derived("op-5", 5, ErdOperationDerivationKind.REDO, u1.opId());
      stubHistory(o2, List.of(o1, o2, u2, u1, r1));

      StepVerifier.create(sut.resolve(UndoRedoAction.REDO, o2.opId()))
          .assertNext(result -> {
            assertThat(result.targetRootOriginalOperation().opId()).isEqualTo("op-2");
            assertThat(result.currentChainTipOperation().opId()).isEqualTo("op-3");
            assertThat(result.currentUndoCandidateOperation().opId()).isEqualTo("op-1");
            assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo("op-2");
            assertThat(result.executionBaseOperation().opId()).isEqualTo("op-2");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("부분 undo 뒤 다른 사용자의 새 original mutation이 추가되면 남아 있던 이전 undo candidate는 SUPERSEDED가 된다")
    void returnsSupersededForOlderUndoCandidateAfterAnotherUsersNewOriginal() {
      var o1 = originalBy("op-1", 1, "user-a");
      var o2 = originalBy("op-2", 2, "user-a");
      var u2 = derivedBy("op-3", 3, ErdOperationDerivationKind.UNDO, o2.opId(), "user-a");
      var o3 = originalBy("op-4", 4, "user-b");
      stubHistory(o1, List.of(o1, o2, u2, o3));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, o1.opId()))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.SUPERSEDED))
          .verify();
    }

    @Test
    @DisplayName("저장된 undo history가 현재 undo frontier를 어기면 IllegalStateException으로 감지한다")
    void throwsWhenPersistedUndoHistoryBreaksLinearStackOrder() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var invalidU1 = derived("op-3", 3, ErdOperationDerivationKind.UNDO, o1.opId());
      stubHistory(o1, List.of(o1, o2, invalidU1));

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, o1.opId()))
          .expectErrorSatisfies(error -> assertThat(error)
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("current undo candidate"))
          .verify();
    }

    @Test
    @DisplayName("저장된 redo history가 현재 redo frontier를 어기면 IllegalStateException으로 감지한다")
    void throwsWhenPersistedRedoHistoryBreaksLinearStackOrder() {
      var o1 = original("op-1", 1);
      var o2 = original("op-2", 2);
      var u2 = derived("op-3", 3, ErdOperationDerivationKind.UNDO, o2.opId());
      var o3 = original("op-4", 4);
      var invalidR2 = derived("op-5", 5, ErdOperationDerivationKind.REDO, u2.opId());
      stubHistory(o2, List.of(o1, o2, u2, o3, invalidR2));

      StepVerifier.create(sut.resolve(UndoRedoAction.REDO, o2.opId()))
          .expectErrorSatisfies(error -> assertThat(error)
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("current redo candidate"))
          .verify();
    }

    @Test
    @DisplayName("target opId가 비어 있으면 INVALID_VALUE를 반환한다")
    void returnsInvalidValueWhenTargetIsBlank() {
      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, " "))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.INVALID_VALUE))
          .verify();
    }

    @Test
    @DisplayName("target opId가 없으면 NOT_FOUND를 반환한다")
    void returnsNotFoundWhenTargetMissing() {
      given(getErdOperationByIdPort.findOperationById(anyString()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.resolve(UndoRedoAction.UNDO, "missing"))
          .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.NOT_FOUND))
          .verify();
    }

  }

  private void stubHistory(ErdOperationLog targetOperation, List<ErdOperationLog> history) {
    given(getErdOperationByIdPort.findOperationById(targetOperation.opId()))
        .willReturn(Mono.just(targetOperation));
    given(getErdOperationsBySchemaIdPort.findOperationsBySchemaIdOrderByCommittedRevisionAsc(SCHEMA_ID))
        .willReturn(Mono.just(history));
  }

  private ErdOperationLog original(String opId, long revision) {
    return originalBy(opId, revision, "user-1");
  }

  private ErdOperationLog derived(
      String opId,
      long revision,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId) {
    return derivedBy(opId, revision, derivationKind, derivedFromOpId, "user-1");
  }

  private ErdOperationLog originalBy(String opId, long revision, String actorUserId) {
    return operation(opId, revision, ErdOperationDerivationKind.ORIGINAL, null, actorUserId);
  }

  private ErdOperationLog derivedBy(
      String opId,
      long revision,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId,
      String actorUserId) {
    return operation(opId, revision, derivationKind, derivedFromOpId, actorUserId);
  }

  private ErdOperationLog operation(
      String opId,
      long revision,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId,
      String actorUserId) {
    return new ErdOperationLog(
        opId,
        PROJECT_ID,
        SCHEMA_ID,
        ErdOperationType.CHANGE_SCHEMA_NAME,
        revision,
        revision - 1,
        null,
        "session-1",
        actorUserId,
        derivationKind,
        derivedFromOpId,
        ErdOperationLifecycleState.COMMITTED,
        "{}",
        "{}",
        "[]",
        "[]");
  }

}
