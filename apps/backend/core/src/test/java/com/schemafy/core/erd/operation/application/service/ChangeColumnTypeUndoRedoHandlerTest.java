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
import com.schemafy.core.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.fixture.ColumnFixture;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse.FkColumnTypeRevert;
import com.schemafy.core.erd.operation.application.inverse.InversePayload;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.vendor.application.service.DatatypePolicyResolver;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
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
@DisplayName("ChangeColumnTypeUndoRedoHandler")
class ChangeColumnTypeUndoRedoHandlerTest {

  @Mock
  ChangeColumnTypePort changeColumnTypePort;

  @Mock
  ChangeColumnMetaPort changeColumnMetaPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  DatatypePolicyResolver datatypePolicyResolver;

  private JsonCodec jsonCodec;
  private ChangeColumnTypeUndoRedoHandler sut;

  @BeforeEach
  void setUp() {
    jsonCodec = new JsonCodec(new ObjectMapper().findAndRegisterModules());
    sut = new ChangeColumnTypeUndoRedoHandler(
        jsonCodec,
        ErdMutationCoordinator.noop(),
        changeColumnTypePort,
        changeColumnMetaPort,
        getColumnByIdPort,
        datatypePolicyResolver);
  }

  @Test
  @DisplayName("현재 policy가 금지한 과거 타입은 undo와 redo 모두 거부한다")
  void rejectsTypeForbiddenByCurrentPolicyForUndoAndRedo() {
    Column current = ColumnFixture.intColumn();
    var inverse = new ChangeColumnTypeInverse(
        current.id(),
        "VARCHAR",
        new ColumnTypeArguments(255, null, null),
        List.of());
    given(getColumnByIdPort.findColumnById(current.id()))
        .willReturn(Mono.just(current));
    given(datatypePolicyResolver.resolve(COLUMN, current.id()))
        .willReturn(Mono.just(intOnlyPolicy()));

    StepVerifier.create(sut.undo(resolved(UndoRedoAction.UNDO, inverse)))
        .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.DATA_TYPE_INVALID))
        .verify();
    StepVerifier.create(sut.redo(resolved(UndoRedoAction.REDO, inverse)))
        .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.DATA_TYPE_INVALID))
        .verify();

    then(changeColumnTypePort).shouldHaveNoInteractions();
    then(changeColumnMetaPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("하나의 FK 복원 상태가 금지되면 direct와 모든 FK 쓰기를 거부한다")
  void rejectsAllWritesWhenAnyFkTargetIsInvalid() {
    Column current = ColumnFixture.intColumn();
    Column fkCurrent = new Column(
        "fk-column-1", "fk-table-1", "fk", "INT", null,
        0, true, null, null, null);
    var inverse = new ChangeColumnTypeInverse(
        current.id(),
        "BIGINT",
        null,
        List.of(new FkColumnTypeRevert(
            fkCurrent.id(),
            "VARCHAR",
            new ColumnTypeArguments(255, null, null),
            "utf8mb4",
            "utf8mb4_general_ci")));
    given(getColumnByIdPort.findColumnById(current.id()))
        .willReturn(Mono.just(current));
    given(getColumnByIdPort.findColumnById(fkCurrent.id()))
        .willReturn(Mono.just(fkCurrent));
    given(datatypePolicyResolver.resolve(COLUMN, current.id()))
        .willReturn(Mono.just(DbVendorFixture.defaultDatatypePolicy()));

    StepVerifier.create(sut.undo(resolved(UndoRedoAction.UNDO, inverse)))
        .expectErrorMatches(DomainException.hasErrorCode(
            ColumnErrorCode.AUTO_INCREMENT_NOT_ALLOWED))
        .verify();

    then(changeColumnTypePort).shouldHaveNoInteractions();
    then(changeColumnMetaPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("모든 복원 상태가 유효하면 전체 상태를 쓰고 forward inverse를 만든다")
  void restoresAllTargetsAndCapturesForwardInverse() {
    Column current = new Column(
        ColumnFixture.DEFAULT_ID, ColumnFixture.DEFAULT_TABLE_ID, "id", "BIGINT", null,
        0, false, null, null, null);
    Column fkCurrent = new Column(
        "fk-column-1", "fk-table-1", "fk", "BIGINT", null,
        0, false, null, null, null);
    var inverse = new ChangeColumnTypeInverse(
        current.id(),
        "INTEGER",
        null,
        List.of(new FkColumnTypeRevert(
            fkCurrent.id(),
            "VARCHAR",
            new ColumnTypeArguments(255, null, null),
            "utf8mb4",
            "utf8mb4_general_ci")));
    given(getColumnByIdPort.findColumnById(current.id()))
        .willReturn(Mono.just(current));
    given(getColumnByIdPort.findColumnById(fkCurrent.id()))
        .willReturn(Mono.just(fkCurrent));
    given(datatypePolicyResolver.resolve(COLUMN, current.id()))
        .willReturn(Mono.just(DbVendorFixture.defaultDatatypePolicy()));
    given(changeColumnTypePort.changeColumnType(any(), any(), any()))
        .willReturn(Mono.empty());
    given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.redo(resolved(UndoRedoAction.REDO, inverse)))
        .assertNext(result -> assertThat(result.inversePayload()).isEqualTo(
            new ChangeColumnTypeInverse(
                current.id(),
                current.dataType(),
                current.typeArguments(),
                List.of(new FkColumnTypeRevert(
                    fkCurrent.id(),
                    fkCurrent.dataType(),
                    fkCurrent.typeArguments(),
                    fkCurrent.charset(),
                    fkCurrent.collation())))))
        .verifyComplete();

    then(changeColumnTypePort).should()
        .changeColumnType(current.id(), "INT", null);
    then(changeColumnTypePort).should()
        .changeColumnType(fkCurrent.id(), "VARCHAR", new ColumnTypeArguments(255, null, null));
    then(changeColumnMetaPort).should()
        .changeColumnMeta(
            eq(fkCurrent.id()), isNull(), eq("utf8mb4"), eq("utf8mb4_general_ci"), isNull());
  }

  private DatatypePolicy intOnlyPolicy() {
    return new DatatypePolicy(
        2,
        "mysql",
        null,
        ">= 8.0 < 9.0",
        List.of(DbVendorFixture.defaultDatatypePolicy().find("INT").orElseThrow()));
  }

  private ResolvedUndoRedoEligibility resolved(
      UndoRedoAction action,
      InversePayload inversePayload) {
    ErdOperationLog operation = new ErdOperationLog(
        "op-1",
        "project-1",
        "schema-1",
        ErdOperationType.CHANGE_COLUMN_TYPE,
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
