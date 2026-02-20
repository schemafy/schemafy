package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
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
@DisplayName("AddRelationshipColumnService")
class AddRelationshipColumnServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String NEW_PK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5NPK";
  private static final String NEW_FK_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5NFK";

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateRelationshipColumnPort createRelationshipColumnPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  AddRelationshipColumnService sut;

  @Nested
  @DisplayName("addRelationshipColumn 메서드는")
  class AddRelationshipColumn {

    @Test
    @DisplayName("관계에 컬럼을 추가한다")
    void addsColumnToRelationship() {
      var command = RelationshipFixture.addColumnCommand(
          RelationshipFixture.DEFAULT_ID, NEW_PK_COLUMN_ID, NEW_FK_COLUMN_ID, 1);
      var relationship = RelationshipFixture.defaultRelationship();
      var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
      var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
      var existingColumn = RelationshipFixture.defaultRelationshipColumn();
      var newFkColumn = ColumnFixture.columnWithId(NEW_FK_COLUMN_ID);
      var newPkColumn = ColumnFixture.columnWithId(NEW_PK_COLUMN_ID);
      var existingFkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
      var existingPkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(existingColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingFkColumn, newFkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingPkColumn, newPkColumn)));
      given(ulidGeneratorPort.generate())
          .willReturn("new_column_id");
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addRelationshipColumn(command))
          .assertNext(result -> {
            var payload = result.result();
            assertThat(payload.relationshipColumnId()).isEqualTo("new_column_id");
            assertThat(payload.pkColumnId()).isEqualTo(NEW_PK_COLUMN_ID);
            assertThat(payload.fkColumnId()).isEqualTo(NEW_FK_COLUMN_ID);
            assertThat(payload.seqNo()).isEqualTo(1);
          })
          .verifyComplete();

      then(createRelationshipColumnPort).should().createRelationshipColumn(any(RelationshipColumn.class));
    }

    @Test
    @DisplayName("seqNo가 없으면 마지막 위치로 자동 추가한다")
    void addsColumnWithAutoSeqNoWhenMissing() {
      AddRelationshipColumnCommand command = new AddRelationshipColumnCommand(
          RelationshipFixture.DEFAULT_ID,
          NEW_PK_COLUMN_ID,
          NEW_FK_COLUMN_ID,
          null);
      var relationship = RelationshipFixture.defaultRelationship();
      var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
      var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
      var existingColumn = RelationshipFixture.defaultRelationshipColumn();
      var newFkColumn = ColumnFixture.columnWithId(NEW_FK_COLUMN_ID);
      var newPkColumn = ColumnFixture.columnWithId(NEW_PK_COLUMN_ID);
      var existingFkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
      var existingPkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(existingColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingFkColumn, newFkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingPkColumn, newPkColumn)));
      given(ulidGeneratorPort.generate())
          .willReturn("new_column_id");
      given(createRelationshipColumnPort.createRelationshipColumn(any(RelationshipColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addRelationshipColumn(command))
          .assertNext(result -> assertThat(result.result().seqNo()).isEqualTo(1))
          .verifyComplete();
    }

    @Test
    @DisplayName("관계가 존재하지 않으면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      var command = RelationshipFixture.addColumnCommand();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.addRelationshipColumn(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.NOT_FOUND))
          .verify();

      then(createRelationshipColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExists() {
      var command = RelationshipFixture.addColumnCommand(
          RelationshipFixture.DEFAULT_ID, "non_existent_pk", "non_existent_fk", 1);
      var relationship = RelationshipFixture.defaultRelationship();
      var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
      var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
      var existingColumn = RelationshipFixture.defaultRelationshipColumn();
      var existingFkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
      var existingPkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(existingColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingFkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingPkColumn)));

      StepVerifier.create(sut.addRelationshipColumn(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.COLUMN_NOT_FOUND))
          .verify();

      then(createRelationshipColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 중복되면 예외가 발생한다")
    void throwsWhenColumnDuplicate() {
      var command = RelationshipFixture.addColumnCommand(
          RelationshipFixture.DEFAULT_ID,
          RelationshipFixture.DEFAULT_PK_COLUMN_ID,
          RelationshipFixture.DEFAULT_FK_COLUMN_ID,
          1);
      var relationship = RelationshipFixture.defaultRelationship();
      var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
      var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
      var existingColumn = RelationshipFixture.defaultRelationshipColumn();
      var existingFkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
      var existingPkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(existingColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingFkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingPkColumn)));

      StepVerifier.create(sut.addRelationshipColumn(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.COLUMN_DUPLICATE))
          .verify();

      then(createRelationshipColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("seqNo가 연속적이지 않으면 예외가 발생한다")
    void throwsWhenSeqNoNotContiguous() {
      var command = RelationshipFixture.addColumnCommand(
          RelationshipFixture.DEFAULT_ID, NEW_PK_COLUMN_ID, NEW_FK_COLUMN_ID, 5);
      var relationship = RelationshipFixture.defaultRelationship();
      var fkTable = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);
      var pkTable = createTable(RelationshipFixture.DEFAULT_PK_TABLE_ID, SCHEMA_ID);
      var existingColumn = RelationshipFixture.defaultRelationshipColumn();
      var newFkColumn = ColumnFixture.columnWithId(NEW_FK_COLUMN_ID);
      var newPkColumn = ColumnFixture.columnWithId(NEW_PK_COLUMN_ID);
      var existingFkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_FK_COLUMN_ID);
      var existingPkColumn = ColumnFixture.columnWithId(RelationshipFixture.DEFAULT_PK_COLUMN_ID);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(fkTable));
      given(getTableByIdPort.findTableById(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(pkTable));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(existingColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_FK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingFkColumn, newFkColumn)));
      given(getColumnsByTableIdPort.findColumnsByTableId(RelationshipFixture.DEFAULT_PK_TABLE_ID))
          .willReturn(Mono.just(List.of(existingPkColumn, newPkColumn)));

      StepVerifier.create(sut.addRelationshipColumn(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.POSITION_INVALID))
          .verify();

      then(createRelationshipColumnPort).shouldHaveNoInteractions();
    }

  }

  private Table createTable(String tableId, String schemaId) {
    return new Table(tableId, schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
  }

}
