package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  CreateColumnPort createColumnPort;

  @Mock
  GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  CreateRelationshipColumnPort createRelationshipColumnPort;

  @InjectMocks
  AddConstraintColumnService sut;

  @Nested
  @DisplayName("addConstraintColumn 메서드는")
  class AddConstraintColumn {

    @Test
    @DisplayName("유효한 요청에 대해 컬럼을 추가한다")
    void addsColumnWithValidRequest() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate())
          .willReturn("new-column-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("table1"))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(result -> {
            assertThat(result.constraintColumnId()).isEqualTo("new-column-id");
            assertThat(result.constraintId()).isEqualTo("constraint1");
            assertThat(result.columnId()).isEqualTo("col2");
            assertThat(result.seqNo()).isEqualTo(1);
            assertThat(result.cascadeCreatedColumns()).isEmpty();
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

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(RuntimeException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 테이블에 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExistsInTable() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "nonexistent", 0);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
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
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
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
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
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
    @DisplayName("PK 컬럼 추가 시 FK 테이블에 FK 컬럼과 RelationshipColumn이 자동 생성된다")
    void cascatesCreatesFkColumnAndRelationshipColumn() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));
      var pkColumn = new Column("col2", "table1", "pk_col2", "INT", null, 1, false, null, null, null);
      var relationship = new Relationship(
          "rel1", "table1", "fk-table1", "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
      var existingFkColumns = List.of(
          new Column("fk-col1", "fk-table1", "existing_col", "INT", null, 0, false, null, null, null));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate())
          .willReturn("new-cc-id", "new-fk-col-id", "new-rel-col-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("table1"))
          .willReturn(Mono.just(List.of(relationship)));
      given(getColumnByIdPort.findColumnById("col2"))
          .willReturn(Mono.just(pkColumn));
      given(getColumnsByTableIdPort.findColumnsByTableId("fk-table1"))
          .willReturn(Mono.just(existingFkColumns));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId("rel1"))
          .willReturn(Mono.just(List.of()));
      given(createColumnPort.createColumn(any(Column.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(result -> {
            assertThat(result.cascadeCreatedColumns()).hasSize(1);
            var cascade = result.cascadeCreatedColumns().get(0);
            assertThat(cascade.fkColumnId()).isEqualTo("new-fk-col-id");
            assertThat(cascade.fkColumnName()).isEqualTo("pk_col2");
            assertThat(cascade.fkTableId()).isEqualTo("fk-table1");
            assertThat(cascade.relationshipColumnId()).isEqualTo("new-rel-col-id");
            assertThat(cascade.relationshipId()).isEqualTo("rel1");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 FK 테이블이 있을 때 모두 cascade 전파된다")
    void cascadesToMultipleFkTables() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));
      var pkColumn = new Column("col2", "table1", "pk_col2", "INT", null, 1, false, null, null, null);
      var rel1 = new Relationship(
          "rel1", "table1", "fk-table1", "fk_1",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
      var rel2 = new Relationship(
          "rel2", "table1", "fk-table2", "fk_2",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_ONE, null);

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate())
          .willReturn("new-cc-id", "fk-col-1", "rel-col-1", "fk-col-2", "rel-col-2");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("table1"))
          .willReturn(Mono.just(List.of(rel1, rel2)));
      given(getColumnByIdPort.findColumnById("col2"))
          .willReturn(Mono.just(pkColumn));
      given(getColumnsByTableIdPort.findColumnsByTableId("fk-table1"))
          .willReturn(Mono.just(List.of()));
      given(getColumnsByTableIdPort.findColumnsByTableId("fk-table2"))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId("rel1"))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId("rel2"))
          .willReturn(Mono.just(List.of()));
      given(createColumnPort.createColumn(any(Column.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(result -> {
            assertThat(result.cascadeCreatedColumns()).hasSize(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("PK가 아닌 constraint 추가 시 cascade가 발생하지 않는다")
    void noCascadeForNonPkConstraint() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col1", 0);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-column-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(result -> {
            assertThat(result.cascadeCreatedColumns()).isEmpty();
          })
          .verifyComplete();

      then(getRelationshipsByPkTableIdPort).should(never())
          .findRelationshipsByPkTableId(any());
    }

    @Test
    @DisplayName("FK 컬럼 이름 충돌 시 suffix가 추가된다")
    void addsNameSuffixOnConflict() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));
      var pkColumn = new Column("col2", "table1", "pk_col", "INT", null, 1, false, null, null, null);
      var relationship = new Relationship(
          "rel1", "table1", "fk-table1", "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
      // FK table already has a column named "pk_col"
      var existingFkColumns = List.of(
          new Column("fk-col1", "fk-table1", "pk_col", "INT", null, 0, false, null, null, null));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId("table1"))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate())
          .willReturn("new-cc-id", "new-fk-col-id", "new-rel-col-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("table1"))
          .willReturn(Mono.just(List.of(relationship)));
      given(getColumnByIdPort.findColumnById("col2"))
          .willReturn(Mono.just(pkColumn));
      given(getColumnsByTableIdPort.findColumnsByTableId("fk-table1"))
          .willReturn(Mono.just(existingFkColumns));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId("rel1"))
          .willReturn(Mono.just(List.of()));
      given(createColumnPort.createColumn(any(Column.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(result -> {
            assertThat(result.cascadeCreatedColumns()).hasSize(1);
            assertThat(result.cascadeCreatedColumns().get(0).fkColumnName())
                .isEqualTo("pk_col_1");
          })
          .verifyComplete();
    }

  }

  private Constraint createConstraint(String id, String tableId, ConstraintKind kind) {
    return new Constraint(id, tableId, "test_constraint", kind, null, null);
  }

}
