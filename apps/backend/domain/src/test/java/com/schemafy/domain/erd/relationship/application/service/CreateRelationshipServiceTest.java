package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.service.PkCascadeHelper;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRelationshipService")
class CreateRelationshipServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String OTHER_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTH";

  private static final String FK_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5FKT";
  private static final String PK_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5PKT";
  private static final String PK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5PKC";
  private static final String REL_ID = "01ARZ3NDEKTSV4RRFFQ69G5REL";

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateRelationshipPort createRelationshipPort;

  @Mock
  CreateRelationshipColumnPort createRelationshipColumnPort;

  @Mock
  CreateColumnPort createColumnPort;

  @Mock
  RelationshipExistsPort relationshipExistsPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  PkCascadeHelper pkCascadeHelper;

  @InjectMocks
  CreateRelationshipService sut;

  @Nested
  @DisplayName("createRelationship 메서드는")
  class CreateRelationship {

    @Test
    @DisplayName("NON_IDENTIFYING 자동 관계를 생성한다")
    void createsNonIdentifyingRelationship() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of());
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");
      var pkColumn = ColumnFixture.columnWithId(PK_COLUMN_ID);
      var pkConstraint = new Constraint(
          "pk-constraint",
          PK_TABLE_ID,
          "pk_pk_table",
          ConstraintKind.PRIMARY_KEY,
          null,
          null);
      var pkConstraintColumn = new ConstraintColumn(
          "pk-cc1",
          "pk-constraint",
          PK_COLUMN_ID,
          0);

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(relationshipExistsPort.existsByFkTableIdAndName(eq(FK_TABLE_ID), any()))
          .willReturn(Mono.just(false));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkConstraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(List.of(pkConstraintColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn(REL_ID, "fk-col-1", "rel-col-1");
      given(createRelationshipPort.createRelationship(any(Relationship.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createColumnPort.createColumn(any(Column.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createRelationship(command))
          .assertNext(result -> {
            assertThat(result.relationshipId()).isEqualTo(REL_ID);
            assertThat(result.name()).isEqualTo("rel_fk_table_to_pk_table");
            assertThat(result.kind()).isEqualTo(RelationshipKind.NON_IDENTIFYING);
            assertThat(result.cardinality()).isEqualTo(Cardinality.ONE_TO_MANY);
          })
          .verifyComplete();

      then(createRelationshipPort).should().createRelationship(any(Relationship.class));
      then(createColumnPort).should().createColumn(any(Column.class));
      then(createRelationshipColumnPort).should().createRelationshipColumn(any(RelationshipColumn.class));
    }

    @Test
    @DisplayName("IDENTIFYING 자동 관계 생성 시 전파를 호출한다")
    void createsIdentifyingRelationshipWithCascade() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of());
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");
      var pkColumn = ColumnFixture.columnWithId(PK_COLUMN_ID);
      var pkConstraint = new Constraint(
          "pk-constraint",
          PK_TABLE_ID,
          "pk_pk_table",
          ConstraintKind.PRIMARY_KEY,
          null,
          null);
      var pkConstraintColumn = new ConstraintColumn(
          "pk-cc1",
          "pk-constraint",
          PK_COLUMN_ID,
          0);

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(relationshipExistsPort.existsByFkTableIdAndName(eq(FK_TABLE_ID), any()))
          .willReturn(Mono.just(false));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkConstraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(List.of(pkConstraintColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn(REL_ID, "fk-col-1", "rel-col-1");
      given(createRelationshipPort.createRelationship(any(Relationship.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createColumnPort.createColumn(any(Column.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(pkCascadeHelper.addPkColumnAndCascade(any(), any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.createRelationship(command))
          .assertNext(result -> assertThat(result.kind()).isEqualTo(RelationshipKind.IDENTIFYING))
          .verifyComplete();

      then(pkCascadeHelper).should().addPkColumnAndCascade(eq(FK_TABLE_ID), any(Column.class), any());
    }

    @Test
    @DisplayName("관계 이름이 중복되면 suffix로 자동 생성한다")
    void appendsSuffixWhenNameExists() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of());
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");
      var pkColumn = ColumnFixture.columnWithId(PK_COLUMN_ID);
      var pkConstraint = new Constraint(
          "pk-constraint",
          PK_TABLE_ID,
          "pk_pk_table",
          ConstraintKind.PRIMARY_KEY,
          null,
          null);
      var pkConstraintColumn = new ConstraintColumn(
          "pk-cc1",
          "pk-constraint",
          PK_COLUMN_ID,
          0);

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(relationshipExistsPort.existsByFkTableIdAndName(eq(FK_TABLE_ID), eq("rel_fk_table_to_pk_table")))
          .willReturn(Mono.just(true));
      given(relationshipExistsPort.existsByFkTableIdAndName(eq(FK_TABLE_ID), eq("rel_fk_table_to_pk_table_1")))
          .willReturn(Mono.just(false));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkConstraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(List.of(pkConstraintColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn(REL_ID, "fk-col-1", "rel-col-1");
      given(createRelationshipPort.createRelationship(any(Relationship.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createColumnPort.createColumn(any(Column.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createRelationship(command))
          .assertNext(result -> assertThat(result.name()).isEqualTo("rel_fk_table_to_pk_table_1"))
          .verifyComplete();
    }

    @Test
    @DisplayName("수동 매핑이 제공되면 예외가 발생한다")
    void throwsWhenManualMappingProvided() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(new CreateRelationshipColumnCommand(PK_COLUMN_ID, "fk-col", 0)));

      StepVerifier.create(sut.createRelationship(command))
          .expectError(InvalidValueException.class)
          .verify();

      then(getTableByIdPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PK 제약이 없으면 예외가 발생한다")
    void throwsWhenPkMissing() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of());
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
          .willReturn(Mono.just(List.of()));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createRelationship(command))
          .expectError(InvalidValueException.class)
          .verify();
    }

    @Test
    @DisplayName("테이블이 다른 스키마에 있으면 예외가 발생한다")
    void throwsWhenTablesInDifferentSchemas() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of());
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, OTHER_SCHEMA_ID, "pk_table");

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));

      StepVerifier.create(sut.createRelationship(command))
          .expectError(RelationshipTargetTableNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("IDENTIFYING 관계 생성 시 순환이 발생하면 예외가 발생한다")
    void throwsWhenIdentifyingCyclicReference() {
      var command = new CreateRelationshipCommand(
          FK_TABLE_ID,
          PK_TABLE_ID,
          null,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of());
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");
      var pkColumn = ColumnFixture.columnWithId(PK_COLUMN_ID);
      var pkConstraint = new Constraint(
          "pk-constraint",
          PK_TABLE_ID,
          "pk_pk_table",
          ConstraintKind.PRIMARY_KEY,
          null,
          null);
      var pkConstraintColumn = new ConstraintColumn(
          "pk-cc1",
          "pk-constraint",
          PK_COLUMN_ID,
          0);
      var existingRelationship = new Relationship(
          "existing",
          FK_TABLE_ID,
          PK_TABLE_ID,
          "existing_fk",
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null);

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
          .willReturn(Mono.just(false));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkConstraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(List.of(pkConstraintColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(PK_TABLE_ID))
          .willReturn(Mono.just(List.of(pkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
          .willReturn(Mono.just(List.of(existingRelationship)));

      StepVerifier.create(sut.createRelationship(command))
          .expectError(RelationshipCyclicReferenceException.class)
          .verify();
    }
  }

  private Table createTable(String tableId, String schemaId, String name) {
    return new Table(tableId, schemaId, name, "utf8mb4", "utf8mb4_general_ci");
  }
}
