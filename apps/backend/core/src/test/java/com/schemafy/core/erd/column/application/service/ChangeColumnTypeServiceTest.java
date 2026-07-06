package com.schemafy.core.erd.column.application.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.fixture.ColumnFixture;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByColumnIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.fixture.SchemaFixture;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnTypeService")
class ChangeColumnTypeServiceTest {

  @Mock
  ChangeColumnTypePort changeColumnTypePort;

  @Mock
  ChangeColumnMetaPort changeColumnMetaPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetRelationshipColumnsByColumnIdPort getRelationshipColumnsByColumnIdPort;

  @Mock
  GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  ChangeColumnTypeService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("changeColumnType 메서드는")
  class ChangeColumnType {

    @BeforeEach
    void setUpFkCheck() {
      lenient()
          .when(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(any()))
          .thenReturn(Mono.just(List.of()));
    }

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("INT에서 BIGINT로 변경한다")
      void changesIntToBigint() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("BIGINT"), any());
      }

      @Test
      @DisplayName("cross-column 검증은 coordinator supplier 내부에서 실행한다")
      void validatesCrossColumnRulesInsideCoordinatorSupplier() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var column = ColumnFixture.intColumn();
        AtomicReference<Supplier<Mono<?>>> mutationSupplierRef = new AtomicReference<>();
        sut.setErdMutationCoordinator(new ErdMutationCoordinator() {

          @Override
          public <T> Mono<MutationResult<T>> coordinate(
              ErdOperationType operationType,
              Object payload,
              Supplier<Mono<MutationResult<T>>> mutationSupplier) {
            mutationSupplierRef.set(mutationSupplier::get);
            return Mono.empty();
          }

        });

        given(getColumnByIdPort.findColumnById(command.columnId()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(column.tableId()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .verifyComplete();

        then(getColumnsByTableIdPort).shouldHaveNoInteractions();
        then(getRelationshipColumnsByColumnIdPort).shouldHaveNoInteractions();
        then(changeColumnTypePort).shouldHaveNoInteractions();

        StepVerifier.create(mutationSupplierRef.get().get())
            .expectNextCount(1)
            .verifyComplete();
      }

      @Test
      @DisplayName("VARCHAR에서 TEXT로 변경한다")
      void changesVarcharToText() {
        var command = ColumnFixture.changeTypeCommand("TEXT", null, null, null);
        var column = ColumnFixture.varcharColumnWithCharset(
            "utf8mb4", "utf8mb4_general_ci");

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("TEXT"), any());
      }

      @Test
      @DisplayName("VARCHAR에서 ENUM(values)으로 변경한다")
      void changesVarcharToEnumWithValues() {
        var command = new ChangeColumnTypeCommand(
            ColumnFixture.DEFAULT_ID,
            "ENUM",
            null,
            null,
            null,
            List.of("ACTIVE", "INACTIVE"));
        var column = ColumnFixture.varcharColumnWithCharset(
            "utf8mb4", "utf8mb4_general_ci");

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("ENUM"), any());
      }

      @Test
      @DisplayName("직접 컬럼 type 결과가 같으면 주변 조회 없이 변경 없이 성공한다")
      void succeedsWithoutCascadeCheckWhenDirectTypeResultIsSame() {
        var command = ColumnFixture.changeTypeCommand("INT", null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(command.columnId()))
            .willReturn(Mono.just(column));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(getColumnsByTableIdPort).shouldHaveNoInteractions();
        then(getRelationshipColumnsByColumnIdPort).shouldHaveNoInteractions();
        then(changeColumnTypePort).shouldHaveNoInteractions();
        then(changeColumnMetaPort).shouldHaveNoInteractions();
        then(getConstraintColumnsByColumnIdPort).shouldHaveNoInteractions();
        then(getRelationshipsByPkTableIdPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("lock 이후 직접 컬럼 type 결과가 같으면 주변 조회 없이 변경 없이 성공한다")
      void succeedsWithoutCascadeCheckWhenLockedDirectTypeResultIsSame() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var initialColumn = ColumnFixture.intColumn();
        var lockedColumn = ColumnFixture.columnWithDataType("BIGINT", null);

        given(getColumnByIdPort.findColumnById(command.columnId()))
            .willReturn(Mono.just(initialColumn), Mono.just(lockedColumn));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextMatches(result -> result.operation() == null && result.noOp())
            .verifyComplete();

        then(getColumnsByTableIdPort).shouldHaveNoInteractions();
        then(getRelationshipColumnsByColumnIdPort).shouldHaveNoInteractions();
        then(changeColumnTypePort).shouldHaveNoInteractions();
        then(changeColumnMetaPort).shouldHaveNoInteractions();
        then(getConstraintColumnsByColumnIdPort).shouldHaveNoInteractions();
        then(getRelationshipsByPkTableIdPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("FK 컬럼의 type을 실제 변경하면 예외가 발생한다")
      void rejectsForeignKeyColumnWhenDirectTypeWouldChange() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var column = ColumnFixture.intColumn();
        var relationshipColumn = new RelationshipColumn(
            "relationship-column-1",
            "relationship-1",
            "pk-column-1",
            column.id(),
            0);

        given(getColumnByIdPort.findColumnById(command.columnId()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(column.tableId()))
            .willReturn(Mono.just(List.of(column)));
        given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.FK_PROTECTED))
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnType(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.NOT_FOUND))
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("INT에서 DECIMAL로 변경할 때 precision이 없으면")
    class WhenIntToDecimalWithoutPrecision {

      @Test
      @DisplayName("ColumnPrecisionRequiredException이 발생한다")
      void throwsColumnPrecisionRequiredException() {
        var command = ColumnFixture.changeTypeCommand("DECIMAL", null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));

        StepVerifier.create(sut.changeColumnType(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.PRECISION_REQUIRED))
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("ENUM으로 변경할 때 values가 없으면")
    class WhenEnumWithoutValues {

      @Test
      @DisplayName("INVALID_VALUE 예외가 발생한다")
      void throwsInvalidValueException() {
        var command = ColumnFixture.changeTypeCommand("ENUM", null, null, null);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));

        StepVerifier.create(sut.changeColumnType(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.INVALID_VALUE))
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("autoIncrement 컬럼을 VARCHAR로 변경하면")
    class WhenAutoIncrementToVarchar {

      @Test
      @DisplayName("ColumnAutoIncrementNotAllowedException이 발생한다")
      void throwsColumnAutoIncrementNotAllowedException() {
        var command = ColumnFixture.changeTypeCommand("VARCHAR", 255, null, null);
        var column = ColumnFixture.intColumnWithAutoIncrement();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));

        StepVerifier.create(sut.changeColumnType(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.AUTO_INCREMENT_NOT_ALLOWED))
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("charset이 있는 컬럼을 INT로 변경하면")
    class WhenCharsetColumnToInt {

      @Test
      @DisplayName("같은 mutation 안에서 charset/collation을 제거한다")
      void clearsCharsetCollationWithinSameMutation() {
        var command = ColumnFixture.changeTypeCommand("INT", null, null, null);
        var column = new Column(
            ColumnFixture.DEFAULT_ID,
            ColumnFixture.DEFAULT_TABLE_ID,
            ColumnFixture.DEFAULT_NAME,
            "VARCHAR",
            new ColumnTypeArguments(255, null, null),
            ColumnFixture.DEFAULT_SEQ_NO,
            false,
            "utf8mb4",
            "utf8mb4_general_ci",
            null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("INT"), any());
        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(null), eq(""), eq(""), eq(null));
      }

    }

    @Nested
    @DisplayName("non-text 컬럼을 text 타입으로 변경하면")
    class WhenNonTextColumnToText {

      @Test
      @DisplayName("table/schema 기본 charset/collation을 같은 mutation 안에서 채운다")
      void resolvesCharsetCollationFromTableAndSchemaDefaults() {
        var command = ColumnFixture.changeTypeCommand("VARCHAR", 255, null, null);
        var column = ColumnFixture.intColumn();
        Table table = new Table(column.tableId(), TableFixture.DEFAULT_SCHEMA_ID, "test_table", null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(getTableByIdPort.findTableById(column.tableId()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(table.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("VARCHAR"), any());
        then(changeColumnMetaPort).should()
            .changeColumnMeta(
                eq(command.columnId()),
                eq(null),
                eq(SchemaFixture.DEFAULT_CHARSET),
                eq(SchemaFixture.DEFAULT_COLLATION),
                eq(null));
      }

    }

    @Nested
    @DisplayName("PK 컬럼의 타입을 변경하면")
    class WhenPkColumnTypeChanged {

      @Test
      @DisplayName("FK 컬럼에도 타입이 전파된다")
      void propagatesTypeToFkColumns() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var pkColumn = ColumnFixture.intColumn();
        var constraintId = "constraint-1";
        var relationshipId = "relationship-1";
        var fkColumnId = "fk-column-1";
        var fkColumn = new Column(
            fkColumnId,
            "fk-table-1",
            "fk_name",
            "INT",
            null,
            0,
            false,
            null,
            null,
            null);

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relationship = new Relationship(relationshipId, pkColumn.tableId(), "fk-table-1",
            "rel_name", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumn.id(),
            fkColumnId, 0);

        given(getColumnByIdPort.findColumnById(pkColumn.id()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnByIdPort.findColumnById(fkColumnId))
            .willReturn(Mono.just(fkColumn));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
            .willReturn(Mono.just(List.of()));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(pkColumn.id()), eq("BIGINT"), any());
        then(changeColumnTypePort).should()
            .changeColumnType(eq(fkColumnId), eq("BIGINT"), any());
        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("다수의 FK 컬럼에 타입이 전파된다")
      void propagatesTypeToMultipleFkColumns() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var pkColumn = ColumnFixture.intColumn();
        var constraintId = "constraint-1";
        var rel1Id = "relationship-1";
        var rel2Id = "relationship-2";
        var fkCol1 = "fk-column-1";
        var fkCol2 = "fk-column-2";
        var fkColumn1 = new Column(
            fkCol1,
            "fk-table-1",
            "fk_name_1",
            "INT",
            null,
            0,
            false,
            null,
            null,
            null);
        var fkColumn2 = new Column(
            fkCol2,
            "fk-table-2",
            "fk_name_2",
            "INT",
            null,
            0,
            false,
            null,
            null,
            null);

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var rel1 = new Relationship(rel1Id, pkColumn.tableId(), "fk-table-1",
            "rel1", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var rel2 = new Relationship(rel2Id, pkColumn.tableId(), "fk-table-2",
            "rel2", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var rc1 = new RelationshipColumn("rc-1", rel1Id, pkColumn.id(), fkCol1, 0);
        var rc2 = new RelationshipColumn("rc-2", rel2Id, pkColumn.id(), fkCol2, 0);

        given(getColumnByIdPort.findColumnById(pkColumn.id()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnByIdPort.findColumnById(fkCol1))
            .willReturn(Mono.just(fkColumn1));
        given(getColumnByIdPort.findColumnById(fkCol2))
            .willReturn(Mono.just(fkColumn2));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkCol1))
            .willReturn(Mono.just(List.of()));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkCol2))
            .willReturn(Mono.just(List.of()));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(rel1, rel2)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(rel1Id))
            .willReturn(Mono.just(List.of(rc1)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(rel2Id))
            .willReturn(Mono.just(List.of(rc2)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should(times(3)).changeColumnType(any(), any(), any());
        then(changeColumnTypePort).should().changeColumnType(eq(fkCol1), eq("BIGINT"), any());
        then(changeColumnTypePort).should().changeColumnType(eq(fkCol2), eq("BIGINT"), any());
        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("VARCHAR에서 INT로 변경하면 FK 컬럼의 charset/collation이 제거된다")
      void clearsCharsetCollationOnFkColumnsWhenChangingToNonTextType() {
        var command = ColumnFixture.changeTypeCommand("INT", null, null, null);
        var pkColumn = ColumnFixture.defaultColumn(); // VARCHAR column
        var constraintId = "constraint-1";
        var relationshipId = "relationship-1";
        var fkColumnId = "fk-column-1";
        var fkColumn = ColumnFixture.varcharColumnWithIdAndCharset(
            fkColumnId, "utf8mb4", "utf8mb4_general_ci");

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relationship = new Relationship(relationshipId, pkColumn.tableId(), "fk-table-1",
            "rel_name", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumn.id(),
            fkColumnId, 0);

        given(getColumnByIdPort.findColumnById(pkColumn.id()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnByIdPort.findColumnById(fkColumnId))
            .willReturn(Mono.just(fkColumn));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
            .willReturn(Mono.just(List.of()));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(fkColumnId), eq(null), eq(""), eq(""), eq(null));
      }

      @Test
      @DisplayName("VARCHAR에서 TEXT로 변경하면 FK 컬럼의 charset/collation이 유지된다")
      void keepsCharsetCollationOnFkColumnsWhenChangingToTextType() {
        var command = ColumnFixture.changeTypeCommand("TEXT", null, null, null);
        var pkColumn = ColumnFixture.varcharColumnWithCharset(
            "utf8mb4", "utf8mb4_general_ci");
        var constraintId = "constraint-1";
        var relationshipId = "relationship-1";
        var fkColumnId = "fk-column-1";
        var fkColumn = ColumnFixture.varcharColumnWithIdAndCharset(
            fkColumnId, "utf8mb4", "utf8mb4_general_ci");

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relationship = new Relationship(relationshipId, pkColumn.tableId(), "fk-table-1",
            "rel_name", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumn.id(),
            fkColumnId, 0);

        given(getColumnByIdPort.findColumnById(pkColumn.id()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnByIdPort.findColumnById(fkColumnId))
            .willReturn(Mono.just(fkColumn));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
            .willReturn(Mono.just(List.of()));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("meta가 비어 있는 text 컬럼을 text 타입으로 변경하면 기본 charset/collation을 전파한다")
      void resolvesMissingTextMetaFromDefaultsDuringCascade() {
        var command = ColumnFixture.changeTypeCommand("TEXT", null, null, null);
        var pkColumn = ColumnFixture.defaultColumn();
        var fkColumnId = "fk-column-1";
        var fkColumn = ColumnFixture.varcharColumnWithIdAndCharset(
            fkColumnId, "latin1", "latin1_swedish_ci");
        var constraintId = "constraint-1";
        var relationshipId = "relationship-1";
        Table table = new Table(pkColumn.tableId(), TableFixture.DEFAULT_SCHEMA_ID, "test_table", null, null);

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relationship = new Relationship(relationshipId, pkColumn.tableId(), "fk-table-1",
            "rel_name", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumn.id(),
            fkColumnId, 0);

        given(getColumnByIdPort.findColumnById(pkColumn.id()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnByIdPort.findColumnById(fkColumnId))
            .willReturn(Mono.just(fkColumn));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(getTableByIdPort.findTableById(pkColumn.tableId()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(table.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
            .willReturn(Mono.just(List.of()));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(pkColumn.id()), eq(null),
                eq(SchemaFixture.DEFAULT_CHARSET), eq(SchemaFixture.DEFAULT_COLLATION), eq(null));
        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(fkColumnId), eq(null),
                eq(SchemaFixture.DEFAULT_CHARSET), eq(SchemaFixture.DEFAULT_COLLATION), eq(null));
      }

      @Test
      @DisplayName("INT에서 VARCHAR로 변경하면 FK 컬럼에도 해석된 charset/collation이 전파된다")
      void propagatesResolvedCharsetCollationOnTextTarget() {
        var command = ColumnFixture.changeTypeCommand("VARCHAR", 255, null, null);
        var pkColumn = ColumnFixture.intColumn();
        var fkColumnId = "fk-column-1";
        var fkColumn = new Column(
            fkColumnId,
            "fk-table-1",
            "fk_name",
            "INT",
            null,
            0,
            false,
            null,
            null,
            null);
        var constraintId = "constraint-1";
        var relationshipId = "relationship-1";
        Table table = new Table(pkColumn.tableId(), TableFixture.DEFAULT_SCHEMA_ID, "test_table", null, null);

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relationship = new Relationship(relationshipId, pkColumn.tableId(), "fk-table-1",
            "rel_name", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumn.id(),
            fkColumnId, 0);

        given(getColumnByIdPort.findColumnById(pkColumn.id()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnByIdPort.findColumnById(fkColumnId))
            .willReturn(Mono.just(fkColumn));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(getTableByIdPort.findTableById(pkColumn.tableId()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(table.schemaId()))
            .willReturn(Mono.just(SchemaFixture.defaultSchema()));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
            .willReturn(Mono.just(List.of()));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(pkColumn.id()), eq(null),
                eq(SchemaFixture.DEFAULT_CHARSET), eq(SchemaFixture.DEFAULT_COLLATION), eq(null));
        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(fkColumnId), eq(null),
                eq(SchemaFixture.DEFAULT_CHARSET), eq(SchemaFixture.DEFAULT_COLLATION), eq(null));
      }

    }

    @Nested
    @DisplayName("다단계 FK 체인에서 PK 컬럼의 타입을 변경하면")
    class WhenMultiLevelCascade {

      @Test
      @DisplayName("하위 FK 컬럼까지 재귀적으로 전파된다")
      void propagatesTypeRecursivelyToDownstreamFkColumns() {
        var colAId = ColumnFixture.DEFAULT_ID;
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);

        var colA = new Column(colAId, "table-a", "pk_a", "INT", null, 0, false, null, null, null);
        var colB = new Column("col-b", "table-b", "fk_b", "INT", null, 0, false, null, null, null);
        var colC = new Column("col-c", "table-c", "fk_c", "INT", null, 0, false, null, null, null);

        var ccA = new ConstraintColumn("cc-a", "cst-a", colAId, 0);
        var cstA = new Constraint("cst-a", "table-a", "pk_a", ConstraintKind.PRIMARY_KEY, null, null);
        var relAB = new Relationship("rel-ab", "table-a", "table-b",
            "rel_ab", RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var rcAB = new RelationshipColumn("rc-ab", "rel-ab", colAId, "col-b", 0);

        var ccB = new ConstraintColumn("cc-b", "cst-b", "col-b", 0);
        var cstB = new Constraint("cst-b", "table-b", "pk_b", ConstraintKind.PRIMARY_KEY, null, null);
        var relBC = new Relationship("rel-bc", "table-b", "table-c",
            "rel_bc", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var rcBC = new RelationshipColumn("rc-bc", "rel-bc", "col-b", "col-c", 0);

        given(getColumnByIdPort.findColumnById(colAId)).willReturn(Mono.just(colA));
        given(getColumnByIdPort.findColumnById("col-b")).willReturn(Mono.just(colB));
        given(getColumnByIdPort.findColumnById("col-c")).willReturn(Mono.just(colC));
        given(getColumnsByTableIdPort.findColumnsByTableId("table-a"))
            .willReturn(Mono.just(List.of(colA)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());

        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(colAId))
            .willReturn(Mono.just(List.of(ccA)));
        given(getConstraintByIdPort.findConstraintById("cst-a"))
            .willReturn(Mono.just(cstA));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("table-a"))
            .willReturn(Mono.just(List.of(relAB)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId("rel-ab"))
            .willReturn(Mono.just(List.of(rcAB)));

        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId("col-b"))
            .willReturn(Mono.just(List.of(ccB)));
        given(getConstraintByIdPort.findConstraintById("cst-b"))
            .willReturn(Mono.just(cstB));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("table-b"))
            .willReturn(Mono.just(List.of(relBC)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId("rel-bc"))
            .willReturn(Mono.just(List.of(rcBC)));

        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId("col-c"))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should().changeColumnType(eq(colAId), eq("BIGINT"), any());
        then(changeColumnTypePort).should().changeColumnType(eq("col-b"), eq("BIGINT"), any());
        then(changeColumnTypePort).should().changeColumnType(eq("col-c"), eq("BIGINT"), any());
      }

    }

    @Nested
    @DisplayName("비-PK 컬럼의 타입을 변경하면")
    class WhenNonPkColumnTypeChanged {

      @Test
      @DisplayName("FK 전파가 발생하지 않는다")
      void doesNotPropagate() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var column = ColumnFixture.intColumn();
        var constraintId = "constraint-1";

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, column.id(), 0);
        var constraint = new Constraint(constraintId, column.tableId(), "uq_constraint",
            ConstraintKind.UNIQUE, null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(column.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));

        StepVerifier.create(sut.changeColumnType(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnTypePort).should(times(1)).changeColumnType(any(), any(), any());
        then(getRelationshipsByPkTableIdPort).should(never()).findRelationshipsByPkTableId(any());
      }

    }

  }

}
