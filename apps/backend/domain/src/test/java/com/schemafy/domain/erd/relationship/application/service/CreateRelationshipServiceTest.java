package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipEmptyException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameInvalidException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateRelationshipService")
class CreateRelationshipServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String OTHER_SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTH";

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateRelationshipPort createRelationshipPort;

  @Mock
  CreateRelationshipColumnPort createRelationshipColumnPort;

  @Mock
  RelationshipExistsPort relationshipExistsPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  @InjectMocks
  CreateRelationshipService sut;

  @Nested
  @DisplayName("createRelationship 메서드는")
  class CreateRelationship {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("NON_IDENTIFYING 관계를 생성한다")
      void createsNonIdentifyingRelationship() {
        var command = RelationshipFixture.createCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
        var fkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
        var pkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(List.of(fkColumn)));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
            .willReturn(Mono.just(List.of()));
        given(ulidGeneratorPort.generate())
            .willReturn(RelationshipFixture.DEFAULT_ID, RelationshipFixture.DEFAULT_COLUMN_ID);
        given(createRelationshipPort.createRelationship(any(Relationship.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createRelationship(command))
            .assertNext(result -> {
              assertThat(result.relationshipId()).isEqualTo(RelationshipFixture.DEFAULT_ID);
              assertThat(result.name()).isEqualTo(command.name());
              assertThat(result.kind()).isEqualTo(RelationshipKind.NON_IDENTIFYING);
              assertThat(result.cardinality()).isEqualTo(Cardinality.ONE_TO_MANY);
            })
            .verifyComplete();

        then(createRelationshipPort).should().createRelationship(any(Relationship.class));
        then(createRelationshipColumnPort).should().createRelationshipColumn(any(RelationshipColumn.class));
      }

      @Test
      @DisplayName("IDENTIFYING 관계를 생성한다")
      void createsIdentifyingRelationship() {
        var command = RelationshipFixture.createIdentifyingCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
        var fkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
        var pkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(List.of(fkColumn)));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
            .willReturn(Mono.just(List.of()));
        given(ulidGeneratorPort.generate())
            .willReturn(RelationshipFixture.DEFAULT_ID, RelationshipFixture.DEFAULT_COLUMN_ID);
        given(createRelationshipPort.createRelationship(any(Relationship.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createRelationship(command))
            .assertNext(result -> {
              assertThat(result.kind()).isEqualTo(RelationshipKind.IDENTIFYING);
            })
            .verifyComplete();
      }

      @Test
      @DisplayName("여러 컬럼을 매핑하는 관계를 생성한다")
      void createsRelationshipWithMultipleColumns() {
        String fkCol1 = "01ARZ3NDEKTSV4RRFFQ69G5FK1";
        String fkCol2 = "01ARZ3NDEKTSV4RRFFQ69G5FK2";
        String pkCol1 = "01ARZ3NDEKTSV4RRFFQ69G5PK1";
        String pkCol2 = "01ARZ3NDEKTSV4RRFFQ69G5PK2";
        var command = RelationshipFixture.createCommandWithColumns(List.of(
            new CreateRelationshipColumnCommand(pkCol1, fkCol1, 0),
            new CreateRelationshipColumnCommand(pkCol2, fkCol2, 1)));
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
        var fkColumn1 = ColumnFixture.columnWithId(fkCol1);
        var fkColumn2 = ColumnFixture.columnWithId(fkCol2);
        var pkColumn1 = ColumnFixture.columnWithId(pkCol1);
        var pkColumn2 = ColumnFixture.columnWithId(pkCol2);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(List.of(fkColumn1, fkColumn2)));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(List.of(pkColumn1, pkColumn2)));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
            .willReturn(Mono.just(List.of()));
        given(ulidGeneratorPort.generate())
            .willReturn(RelationshipFixture.DEFAULT_ID, "col1", "col2");
        given(createRelationshipPort.createRelationship(any(Relationship.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createRelationship(command))
            .assertNext(result -> {
              assertThat(result.relationshipId()).isEqualTo(RelationshipFixture.DEFAULT_ID);
            })
            .verifyComplete();

        then(createRelationshipColumnPort).should(org.mockito.Mockito.times(2))
            .createRelationshipColumn(any(RelationshipColumn.class));
      }

    }

    @Nested
    @DisplayName("FK 테이블이 존재하지 않으면")
    class WhenFkTableNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.createCommand();

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipTargetTableNotExistException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("PK 테이블이 존재하지 않으면")
    class WhenPkTableNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.createCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipTargetTableNotExistException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("테이블들이 다른 스키마에 있으면")
    class WhenTablesInDifferentSchemas {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.createCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, OTHER_SCHEMA_ID);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipTargetTableNotExistException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("이름이 유효하지 않으면")
    class WithInvalidName {

      @Test
      @DisplayName("빈 이름일 때 예외가 발생한다")
      void throwsWhenEmptyName() {
        var command = RelationshipFixture.createCommandWithName("");

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipNameInvalidException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("공백 이름일 때 예외가 발생한다")
      void throwsWhenBlankName() {
        var command = RelationshipFixture.createCommandWithName("   ");

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipNameInvalidException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("이름이 중복되면")
    class WithDuplicateName {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.createCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipNameDuplicateException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("컬럼이 없으면")
    class WithEmptyColumns {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.createCommandWithColumns(List.of());

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipEmptyException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("FK 컬럼이 없을 때 예외가 발생한다")
      void throwsWhenFkColumnNotExists() {
        var command = RelationshipFixture.createCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
        var pkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(List.of()));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipColumnNotExistException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("컬럼이 중복되면")
    class WithDuplicateColumns {

      @Test
      @DisplayName("FK 컬럼 중복 시 예외가 발생한다")
      void throwsWhenFkColumnDuplicate() {
        String fkCol = RelationshipFixture.DEFAULT_FK_COLUMN_ID;
        String pkCol1 = "01ARZ3NDEKTSV4RRFFQ69G5PK1";
        String pkCol2 = "01ARZ3NDEKTSV4RRFFQ69G5PK2";
        var command = RelationshipFixture.createCommandWithColumns(List.of(
            new CreateRelationshipColumnCommand(pkCol1, fkCol, 0),
            new CreateRelationshipColumnCommand(pkCol2, fkCol, 1)));
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
        var fkColumn = ColumnFixture.columnWithId(fkCol);
        var pkColumn1 = ColumnFixture.columnWithId(pkCol1);
        var pkColumn2 = ColumnFixture.columnWithId(pkCol2);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(List.of(fkColumn)));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(List.of(pkColumn1, pkColumn2)));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipColumnDuplicateException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("IDENTIFYING 관계 생성 시 순환이 발생하면")
    class WhenIdentifyingCyclicReference {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.createIdentifyingCommand();
        var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
        var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
        var fkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
        var pkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);
        var existingRelationship = new Relationship(
            "existing",
            RelationshipFixture.DEFAULT_FK_TABLE_ID,
            RelationshipFixture.DEFAULT_PK_TABLE_ID,
            "existing_fk",
            RelationshipKind.IDENTIFYING,
            Cardinality.ONE_TO_MANY,
            null);

        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(fkTable));
        given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(pkTable));
        given(relationshipExistsPort.existsByFkTableIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
            .willReturn(Mono.just(List.of(fkColumn)));
        given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(any()))
            .willReturn(Mono.just(List.of(existingRelationship)));

        StepVerifier.create(sut.createRelationship(command))
            .expectError(RelationshipCyclicReferenceException.class)
            .verify();

        then(createRelationshipPort).shouldHaveNoInteractions();
      }

    }

  }

  private Table createTable(String tableId, String schemaId) {
    return new Table(tableId, schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
  }

}
