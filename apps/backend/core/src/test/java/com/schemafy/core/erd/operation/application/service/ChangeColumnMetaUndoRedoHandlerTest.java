package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.fixture.ColumnFixture;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnMetaInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnMetaInverse.FkColumnMetaRevert;
import com.schemafy.core.erd.operation.application.inverse.InversePayload;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.vendor.application.service.DatatypePolicyResolver;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnMetaUndoRedoHandler")
class ChangeColumnMetaUndoRedoHandlerTest {

  @Mock
  ChangeColumnMetaPort changeColumnMetaPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  DatatypePolicyResolver datatypePolicyResolver;

  private JsonCodec jsonCodec;
  private ChangeColumnMetaUndoRedoHandler sut;

  @BeforeEach
  void setUp() {
    jsonCodec = new JsonCodec(new ObjectMapper().findAndRegisterModules());
    sut = new ChangeColumnMetaUndoRedoHandler(
        jsonCodec,
        ErdMutationCoordinator.noop(),
        changeColumnMetaPort,
        getColumnByIdPort,
        datatypePolicyResolver);
  }

  @Test
  @DisplayName("현재 policy가 금지한 과거 meta 상태는 undo와 redo 모두 거부한다")
  void rejectsMetaForbiddenByCurrentPolicyForUndoAndRedo() {
    Column current = ColumnFixture.intColumn();
    var inverse = new ChangeColumnMetaInverse(
        current.id(),
        null,
        "utf8mb4",
        "utf8mb4_general_ci",
        null,
        List.of());
    given(getColumnByIdPort.findColumnById(current.id()))
        .willReturn(Mono.just(current));
    given(datatypePolicyResolver.resolve(COLUMN, current.id()))
        .willReturn(Mono.just(DbVendorFixture.defaultDatatypePolicy()));

    StepVerifier.create(sut.undo(resolved(UndoRedoAction.UNDO, inverse)))
        .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.CHARSET_NOT_ALLOWED))
        .verify();
    StepVerifier.create(sut.redo(resolved(UndoRedoAction.REDO, inverse)))
        .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.CHARSET_NOT_ALLOWED))
        .verify();

    then(changeColumnMetaPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("하나의 FK 복원 상태가 금지되면 direct와 모든 FK 쓰기를 거부한다")
  void rejectsAllWritesWhenAnyFkTargetIsInvalid() {
    Column current = ColumnFixture.defaultColumn();
    Column fkCurrent = new Column(
        "fk-column-1", "fk-table-1", "fk", "INT", null,
        0, false, null, null, null);
    var inverse = new ChangeColumnMetaInverse(
        current.id(),
        null,
        "utf8mb4",
        "utf8mb4_general_ci",
        null,
        List.of(new FkColumnMetaRevert(
            fkCurrent.id(), "utf8mb4", "utf8mb4_general_ci")));
    given(getColumnByIdPort.findColumnById(current.id()))
        .willReturn(Mono.just(current));
    given(getColumnByIdPort.findColumnById(fkCurrent.id()))
        .willReturn(Mono.just(fkCurrent));
    given(datatypePolicyResolver.resolve(COLUMN, current.id()))
        .willReturn(Mono.just(DbVendorFixture.defaultDatatypePolicy()));

    StepVerifier.create(sut.undo(resolved(UndoRedoAction.UNDO, inverse)))
        .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.CHARSET_NOT_ALLOWED))
        .verify();

    then(changeColumnMetaPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("모든 복원 상태가 유효하면 전체 상태를 쓰고 forward inverse를 만든다")
  void restoresAllTargetsAndCapturesForwardInverse() {
    Column current = new Column(
        ColumnFixture.DEFAULT_ID,
        ColumnFixture.DEFAULT_TABLE_ID,
        "name",
        "VARCHAR",
        new ColumnTypeArguments(255, null, null),
        0,
        false,
        "utf8mb4",
        "utf8mb4_unicode_ci",
        "new comment");
    Column fkCurrent = new Column(
        "fk-column-1",
        "fk-table-1",
        "fk",
        "VARCHAR",
        new ColumnTypeArguments(255, null, null),
        0,
        false,
        "utf8mb4",
        "utf8mb4_unicode_ci",
        null);
    var inverse = new ChangeColumnMetaInverse(
        current.id(),
        null,
        "latin1",
        "latin1_swedish_ci",
        "old comment",
        List.of(new FkColumnMetaRevert(
            fkCurrent.id(), "latin1", "latin1_swedish_ci")));
    given(getColumnByIdPort.findColumnById(current.id()))
        .willReturn(Mono.just(current));
    given(getColumnByIdPort.findColumnById(fkCurrent.id()))
        .willReturn(Mono.just(fkCurrent));
    given(datatypePolicyResolver.resolve(COLUMN, current.id()))
        .willReturn(Mono.just(DbVendorFixture.defaultDatatypePolicy()));
    given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.redo(resolved(UndoRedoAction.REDO, inverse)))
        .assertNext(result -> assertThat(result.inversePayload()).isEqualTo(
            new ChangeColumnMetaInverse(
                current.id(),
                null,
                current.charset(),
                current.collation(),
                current.comment(),
                List.of(new FkColumnMetaRevert(
                    fkCurrent.id(), fkCurrent.charset(), fkCurrent.collation())))))
        .verifyComplete();

    then(changeColumnMetaPort).should()
        .changeColumnMeta(
            eq(current.id()), isNull(), eq("latin1"), eq("latin1_swedish_ci"), eq("old comment"));
    then(changeColumnMetaPort).should()
        .changeColumnMeta(
            eq(fkCurrent.id()), isNull(), eq("latin1"), eq("latin1_swedish_ci"), isNull());
  }

  private ResolvedUndoRedoEligibility resolved(
      UndoRedoAction action,
      InversePayload inversePayload) {
    ErdOperationLog operation = new ErdOperationLog(
        "op-1",
        "project-1",
        "schema-1",
        ErdOperationType.CHANGE_COLUMN_META,
        2,
        1L,
        "client-op-1",
        "session-1",
        "user-1",
        ErdOperationDerivationKind.ORIGINAL,
        null,
        ErdOperationLifecycleState.COMMITTED,
        "{}",
        jsonCodec.toJson(inversePayload),
        "[]");
    return new ResolvedUndoRedoEligibility(
        action,
        operation,
        operation,
        operation,
        operation,
        operation,
        operation);
  }

}
