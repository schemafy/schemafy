package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.service.PkCascadeHelper.CascadeCreatedInfo;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddConstraintColumnService")
class AddConstraintColumnServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateConstraintColumnPort createConstraintColumnPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  PkCascadeHelper pkCascadeHelper;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  AddConstraintColumnService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("addConstraintColumn 메서드는")
  class AddConstraintColumn {

    @Test
    @DisplayName("유효한 요청에 대해 컬럼을 추가한다")
    void addsColumnWithValidRequest() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate()).willReturn("new-column-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById("col2"))
          .willReturn(
              Mono.just(
                  new Column("col2", "table1", "col2", "INT", null, 1, false, null, null, null)));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(
              result -> {
                var payload = result.result();
                assertThat(payload.constraintColumnId()).isEqualTo("new-column-id");
                assertThat(payload.constraintId()).isEqualTo("constraint1");
                assertThat(payload.columnId()).isEqualTo("col2");
                assertThat(payload.seqNo()).isEqualTo(1);
                assertThat(payload.cascadeCreatedColumns()).isEmpty();
              })
          .verifyComplete();

      then(createConstraintColumnPort).should().createConstraintColumn(any(ConstraintColumn.class));
    }

    @Test
    @DisplayName("음수 seqNo면 예외가 발생한다")
    void throwsWhenNegativeSeqNo() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col1", -1);

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintPositionInvalidException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.addColumnCommand("nonexistent", "col1", 0);

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.empty());

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintNotExistException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 테이블에 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExistsInTable() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "nonexistent", 0);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintColumnNotExistException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복 컬럼 추가 시 예외가 발생한다")
    void throwsWhenDuplicateColumn() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col1", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintColumnDuplicateException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("연속되지 않은 seqNo면 예외가 발생한다")
    void throwsWhenNonContiguousSeqNo() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 5);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintPositionInvalidException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PK 컬럼 추가 시 PkCascadeHelper를 통해 FK 테이블에 FK 컬럼이 자동 생성된다")
    void cascatesCreatesFkColumnAndRelationshipColumn() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));
      var pkColumn = new Column("col2", "table1", "pk_col2", "INT", null, 1, false, null, null, null);

      var cascadeInfo = new CascadeCreatedInfo(
          "new-fk-col-id", "pk_col2", "fk-table1", "new-rel-col-id", "rel1", null, null);

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate()).willReturn("new-cc-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById("col2")).willReturn(Mono.just(pkColumn));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of(cascadeInfo)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(
              result -> {
                var payload = result.result();
                assertThat(payload.cascadeCreatedColumns()).hasSize(1);
                var cascade = payload.cascadeCreatedColumns().get(0);
                assertThat(cascade.fkColumnId()).isEqualTo("new-fk-col-id");
                assertThat(cascade.fkColumnName()).isEqualTo("pk_col2");
                assertThat(cascade.fkTableId()).isEqualTo("fk-table1");
                assertThat(cascade.relationshipColumnId()).isEqualTo("new-rel-col-id");
                assertThat(cascade.relationshipId()).isEqualTo("rel1");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 FK 테이블이 있을 때 PkCascadeHelper를 통해 모두 cascade 전파된다")
    void cascadesToMultipleFkTables() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));
      var pkColumn = new Column("col2", "table1", "pk_col2", "INT", null, 1, false, null, null, null);

      var cascadeInfo1 = new CascadeCreatedInfo(
          "fk-col-1", "pk_col2", "fk-table1", "rel-col-1", "rel1", null, null);
      var cascadeInfo2 = new CascadeCreatedInfo(
          "fk-col-2", "pk_col2", "fk-table2", "rel-col-2", "rel2", "cc-id", "pk-id");

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate()).willReturn("new-cc-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById("col2")).willReturn(Mono.just(pkColumn));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of(cascadeInfo1, cascadeInfo2)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(
              result -> {
                assertThat(result.result().cascadeCreatedColumns()).hasSize(2);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("PK가 아닌 constraint 추가 시 cascade가 발생하지 않는다")
    void noCascadeForNonPkConstraint() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col1", 0);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate()).willReturn("new-column-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(
              result -> {
                assertThat(result.result().cascadeCreatedColumns()).isEmpty();
              })
          .verifyComplete();

      then(pkCascadeHelper).should(never()).cascadeAddPkColumn(any(), any(), any(), any());
    }

    @Test
    @DisplayName("FK 컬럼 이름 충돌 시 PkCascadeHelper가 suffix를 추가한다")
    void addsNameSuffixOnConflict() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"), ColumnFixture.columnWithId("col2"));
      var pkColumn = new Column("col2", "table1", "pk_col", "INT", null, 1, false, null, null, null);

      // PkCascadeHelper handles name suffix logic
      var cascadeInfo = new CascadeCreatedInfo(
          "new-fk-col-id", "pk_col_1", "fk-table1", "new-rel-col-id", "rel1", null, null);

      given(getConstraintByIdPort.findConstraintById(any())).willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate()).willReturn("new-cc-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getColumnByIdPort.findColumnById("col2")).willReturn(Mono.just(pkColumn));
      given(pkCascadeHelper.cascadeAddPkColumn(any(), any(), any(), any()))
          .willReturn(Mono.just(List.of(cascadeInfo)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(
              result -> {
                var payload = result.result();
                assertThat(payload.cascadeCreatedColumns()).hasSize(1);
                assertThat(payload.cascadeCreatedColumns().get(0).fkColumnName())
                    .isEqualTo("pk_col_1");
              })
          .verifyComplete();
    }

  }

  private Constraint createConstraint(String id, String tableId, ConstraintKind kind) {
    return new Constraint(id, tableId, "test_constraint", kind, null, null);
  }

}
