package com.schemafy.core.erd.relationship.application.service;

import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.fixture.ColumnFixture;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.application.service.PkCascadeHelper;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.service.StructuralSnapshotService;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.core.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.relationship.fixture.RelationshipFixture;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.erd.operation.application.service.StructuralSnapshotServiceTestSupport.stubEmptySnapshots;
import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

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
  IdentifierCapabilityResolver identifierCapabilityResolver;

  @Mock
  PkCascadeHelper pkCascadeHelper;

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  StructuralSnapshotService structuralSnapshotService;

  @Spy
  JsonObjectMetadataConverter jsonObjectMetadataConverter = new JsonObjectMetadataConverter(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @InjectMocks
  CreateRelationshipService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    lenient().when(identifierCapabilityResolver.resolve(any(), any()))
        .thenReturn(Mono.just(IdentifierCapabilities.codePoints(64)));
    stubEmptySnapshots(structuralSnapshotService);
  }

  @Nested
  @DisplayName("createRelationship 메서드는")
  class CreateRelationship {

    @Test
    @DisplayName("NON_IDENTIFYING 자동 관계를 생성한다")
    void createsNonIdentifyingRelationship() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          RelationshipFixture.jsonObject("{\"line\": {\"style\": \"solid\"}}"));
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
            var payload = result.result();
            assertThat(payload.relationshipId()).isEqualTo(REL_ID);
            assertThat(payload.name()).isEqualTo("rel_fk_table_to_pk_table");
            assertThat(payload.kind()).isEqualTo(RelationshipKind.NON_IDENTIFYING);
            assertThat(payload.cardinality()).isEqualTo(Cardinality.ONE_TO_MANY);
            assertThat(payload.extra()).isEqualTo("{\"line\":{\"style\":\"solid\"}}");
          })
          .verifyComplete();

      then(createRelationshipPort).should().createRelationship(any(Relationship.class));
      then(createColumnPort).should().createColumn(any(Column.class));
      then(createRelationshipColumnPort).should().createRelationshipColumn(any(RelationshipColumn.class));
    }

    @Test
    @DisplayName("IDENTIFYING 자동 관계 생성 시 전파를 호출한다")
    void createsIdentifyingRelationshipWithCascade() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
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
      given(pkCascadeHelper.addPkColumnAndCascade(any(), any(), any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.createRelationship(command))
          .assertNext(result -> assertThat(result.result().kind()).isEqualTo(RelationshipKind.IDENTIFYING))
          .verifyComplete();

      then(pkCascadeHelper).should()
          .addPkColumnAndCascade(eq(FK_TABLE_ID), any(Column.class), any(), any());
    }

    @Test
    @DisplayName("관계 이름이 중복되면 suffix로 자동 생성한다")
    void appendsSuffixWhenNameExists() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
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
          .assertNext(result -> assertThat(result.result().name()).isEqualTo("rel_fk_table_to_pk_table_1"))
          .verifyComplete();
    }

    @Test
    @DisplayName("긴 관계 이름의 두 자리 suffix 공간을 확보해 자동 생성한다")
    void fitsTwoDigitSuffixWithinIdentifierLimit() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "f".repeat(40));
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "p".repeat(40));
      var pkColumn = ColumnFixture.columnWithId(PK_COLUMN_ID);
      stubSuccessfulRelationshipCreation(fkTable, pkTable, pkColumn, List.of());
      given(relationshipExistsPort.existsByFkTableIdAndName(eq(FK_TABLE_ID), any()))
          .willAnswer(invocation -> {
            String candidate = invocation.getArgument(1);
            return Mono.just(!candidate.endsWith("_10"));
          });

      StepVerifier.create(sut.createRelationship(command))
          .assertNext(result -> {
            String relationshipName = result.result().name();
            assertThat(relationshipName).hasSize(64).endsWith("_10");
          })
          .verifyComplete();

      then(identifierCapabilityResolver).should().resolve(TABLE, FK_TABLE_ID);
    }

    @Test
    @DisplayName("40자 PK 컬럼 이름이 충돌하면 suffix 공간을 확보해 FK 컬럼을 자동 생성한다")
    void fitsFkColumnSuffixWithinLocalColumnLimit() {
      String sourceName = "p".repeat(40);
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, SCHEMA_ID, "pk_table");
      var pkColumn = new Column(
          PK_COLUMN_ID,
          PK_TABLE_ID,
          sourceName,
          "INT",
          null,
          0,
          false,
          null,
          null,
          null);
      var existingFkColumn = new Column(
          "existing-fk-column",
          FK_TABLE_ID,
          sourceName,
          "INT",
          null,
          0,
          false,
          null,
          null,
          null);
      stubSuccessfulRelationshipCreation(fkTable, pkTable, pkColumn, List.of(existingFkColumn));
      given(relationshipExistsPort.existsByFkTableIdAndName(eq(FK_TABLE_ID), any()))
          .willReturn(Mono.just(false));

      StepVerifier.create(sut.createRelationship(command))
          .expectNextCount(1)
          .verifyComplete();

      ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
      then(createColumnPort).should().createColumn(columnCaptor.capture());
      assertThat(columnCaptor.getValue().name())
          .isEqualTo("p".repeat(38) + "_1")
          .hasSize(40);
    }

    @Test
    @DisplayName("PK 제약이 없으면 예외가 발생한다")
    void throwsWhenPkMissing() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
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
          .expectError(DomainException.class)
          .verify();
    }

    @Test
    @DisplayName("FK 테이블이 없으면 관계 대상 테이블 예외가 발생하고 snapshot을 캡처하지 않는다")
    void throwsTargetTableNotFoundWhenFkTableMissingBeforeSnapshot() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.createRelationship(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND))
          .verify();

      then(structuralSnapshotService).should(never()).captureByTableId(any());
      then(structuralSnapshotService).should(never()).captureBySchemaId(any());
    }

    @Test
    @DisplayName("PK 테이블이 없으면 관계 대상 테이블 예외가 발생하고 snapshot을 캡처하지 않는다")
    void throwsTargetTableNotFoundWhenPkTableMissingBeforeSnapshot() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.createRelationship(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND))
          .verify();

      then(structuralSnapshotService).should(never()).captureByTableId(any());
      then(structuralSnapshotService).should(never()).captureBySchemaId(any());
    }

    @Test
    @DisplayName("테이블이 다른 스키마에 있으면 예외가 발생한다")
    void throwsWhenTablesInDifferentSchemas() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
      var fkTable = createTable(FK_TABLE_ID, SCHEMA_ID, "fk_table");
      var pkTable = createTable(PK_TABLE_ID, OTHER_SCHEMA_ID, "pk_table");

      given(getTableByIdPort.findTableById(FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));

      StepVerifier.create(sut.createRelationship(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND))
          .verify();
    }

    @Test
    @DisplayName("IDENTIFYING 관계 생성 시 순환이 발생하면 예외가 발생한다")
    void throwsWhenIdentifyingCyclicReference() {
      var command = new CreateRelationshipCommand(FK_TABLE_ID,
          PK_TABLE_ID,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY, null);
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
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.CYCLIC_REFERENCE))
          .verify();
    }

  }

  private void stubSuccessfulRelationshipCreation(
      Table fkTable,
      Table pkTable,
      Column pkColumn,
      List<Column> existingFkColumns) {
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
    given(getConstraintsByTableIdPort.findConstraintsByTableId(PK_TABLE_ID))
        .willReturn(Mono.just(List.of(pkConstraint)));
    given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
        .willReturn(Mono.just(List.of(pkConstraintColumn)));
    given(getColumnsByTableIdPort.findColumnsByTableId(PK_TABLE_ID))
        .willReturn(Mono.just(List.of(pkColumn)));
    given(getColumnsByTableIdPort.findColumnsByTableId(FK_TABLE_ID))
        .willReturn(Mono.just(existingFkColumns));
    given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(SCHEMA_ID))
        .willReturn(Mono.just(List.of()));
    given(ulidGeneratorPort.generate())
        .willReturn(REL_ID, "fk-col-1", "rel-col-1");
    given(createRelationshipPort.createRelationship(any(Relationship.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(createColumnPort.createColumn(any(Column.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
  }

  private Table createTable(String tableId, String schemaId, String name) {
    return new Table(tableId, schemaId, name, "utf8mb4", "utf8mb4_general_ci");
  }

}
