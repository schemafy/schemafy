package com.schemafy.core.erd.operation.integration;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.AppendErdOperationLogPort;
import com.schemafy.core.erd.operation.application.service.UndoRedoAction;
import com.schemafy.core.erd.operation.application.service.UndoRedoEligibilityService;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.support.ErdProjectIntegrationSupport;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UndoRedoEligibility 통합 테스트")
class UndoRedoEligibilityIntegrationTest extends ErdProjectIntegrationSupport {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  AppendErdOperationLogPort appendErdOperationLogPort;

  @Autowired
  UndoRedoEligibilityService undoRedoEligibilityService;

  @Test
  @DisplayName("persisted operation history에서 선형 undo/redo candidate를 계산한다")
  void resolvesLinearCandidatesFromPersistedHistory() {
    String projectId = createActiveProjectId("undo_redo_eligibility");

    var schemaMutation = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "undo_redo_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block();

    String schemaId = schemaMutation.result().id();

    var o2 = append(opId("op-2"), schemaId, projectId, 2, ErdOperationDerivationKind.ORIGINAL, null);
    var o3 = append(opId("op-3"), schemaId, projectId, 3, ErdOperationDerivationKind.ORIGINAL, null);
    var u3 = append(opId("op-4"), schemaId, projectId, 4, ErdOperationDerivationKind.UNDO, o3.opId());
    var u2 = append(opId("op-5"), schemaId, projectId, 5, ErdOperationDerivationKind.UNDO, o2.opId());
    var r2 = append(opId("op-6"), schemaId, projectId, 6, ErdOperationDerivationKind.REDO, u2.opId());

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, r2.opId()))
        .assertNext(result -> {
          assertThat(result.targetRootOriginalOperation().opId()).isEqualTo(o2.opId());
          assertThat(result.currentChainTipOperation().opId()).isEqualTo(r2.opId());
          assertThat(result.currentUndoCandidateOperation().opId()).isEqualTo(o2.opId());
          assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo(o3.opId());
          assertThat(result.schemaHeadOperation().opId()).isEqualTo(r2.opId());
          assertThat(result.executionBaseOperation().opId()).isEqualTo(r2.opId());
        })
        .verifyComplete();

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.REDO, u3.opId()))
        .assertNext(result -> {
          assertThat(result.targetRootOriginalOperation().opId()).isEqualTo(o3.opId());
          assertThat(result.currentChainTipOperation().opId()).isEqualTo(u3.opId());
          assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo(o3.opId());
          assertThat(result.executionBaseOperation().opId()).isEqualTo(o3.opId());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("부분 undo 뒤 새 original mutation이 추가되면 이전 undo/redo frontier를 무효화한다")
  void invalidatesPriorUndoRedoFrontierAfterNewOriginalMutation() {
    String projectId = createActiveProjectId("undo_redo_frontier_reset");

    var schemaMutation = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "undo_redo_frontier_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block();

    String schemaId = schemaMutation.result().id();

    var o2 = appendBy(opId("op-2"), schemaId, projectId, 2, ErdOperationDerivationKind.ORIGINAL, null, "user-a");
    var o3 = appendBy(opId("op-3"), schemaId, projectId, 3, ErdOperationDerivationKind.ORIGINAL, null, "user-a");
    appendBy(opId("op-4"), schemaId, projectId, 4, ErdOperationDerivationKind.UNDO, o3.opId(), "user-a");
    var o4 = appendBy(opId("op-5"), schemaId, projectId, 5, ErdOperationDerivationKind.ORIGINAL, null, "user-b");

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, o2.opId()))
        .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.SUPERSEDED))
        .verify();

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.REDO, o3.opId()))
        .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.REDO_NOT_ELIGIBLE))
        .verify();

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, o4.opId()))
        .assertNext(result -> {
          assertThat(result.currentUndoCandidateOperation().opId()).isEqualTo(o4.opId());
          assertThat(result.currentRedoCandidateOperation()).isNull();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("여러 번 undo된 persisted history에서는 redo stack 최상단만 redo candidate가 된다")
  void allowsOnlyTopRedoCandidateFromPersistedHistory() {
    String projectId = createActiveProjectId("undo_redo_redo_stack");

    var schemaMutation = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "undo_redo_redo_stack_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block();

    String schemaId = schemaMutation.result().id();

    var o2 = append(opId("op-2"), schemaId, projectId, 2, ErdOperationDerivationKind.ORIGINAL, null);
    var o3 = append(opId("op-3"), schemaId, projectId, 3, ErdOperationDerivationKind.ORIGINAL, null);
    append(opId("op-4"), schemaId, projectId, 4, ErdOperationDerivationKind.UNDO, o3.opId());
    append(opId("op-5"), schemaId, projectId, 5, ErdOperationDerivationKind.UNDO, o2.opId());

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.REDO, o3.opId()))
        .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.REDO_NOT_ELIGIBLE))
        .verify();

    StepVerifier.create(undoRedoEligibilityService.resolve(UndoRedoAction.REDO, o2.opId()))
        .assertNext(result -> {
          assertThat(result.targetRootOriginalOperation().opId()).isEqualTo(o2.opId());
          assertThat(result.currentRedoCandidateOperation().opId()).isEqualTo(o2.opId());
          assertThat(result.executionBaseOperation().opId()).isEqualTo(o2.opId());
        })
        .verifyComplete();
  }

  private ErdOperationLog append(
      String opId,
      String schemaId,
      String projectId,
      long revision,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId) {
    return appendBy(opId, schemaId, projectId, revision, derivationKind, derivedFromOpId, "user-1");
  }

  private ErdOperationLog appendBy(
      String opId,
      String schemaId,
      String projectId,
      long revision,
      ErdOperationDerivationKind derivationKind,
      String derivedFromOpId,
      String actorUserId) {
    var operationLog = new ErdOperationLog(
        opId,
        projectId,
        schemaId,
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

    return appendErdOperationLogPort.append(operationLog).block();
  }

  private String opId(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

}
