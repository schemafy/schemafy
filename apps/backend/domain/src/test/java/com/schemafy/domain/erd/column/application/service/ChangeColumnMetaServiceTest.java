package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.exception.ColumnAutoIncrementNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnCharsetNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.exception.MultipleAutoIncrementColumnException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnMetaService")
class ChangeColumnMetaServiceTest {

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
  GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @InjectMocks
  ChangeColumnMetaService sut;

  @Nested
  @DisplayName("changeColumnMeta 메서드는")
  class ChangeColumnMeta {

    @Nested
    @DisplayName("부분 업데이트 시")
    class WithPartialUpdate {

      @Test
      @DisplayName("autoIncrement만 변경한다")
      void changesOnlyAutoIncrement() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(true), any(), any(), any());
      }

      @Test
      @DisplayName("charset만 변경한다")
      void changesOnlyCharset() {
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", null, null);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(false), eq("utf8mb4"), any(), any());
      }

      @Test
      @DisplayName("comment만 변경한다")
      void changesOnlyComment() {
        var command = ColumnFixture.changeMetaCommand(null, null, null, "New comment");
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(false), any(), any(), eq("New comment"));
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(ColumnNotExistException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("비정수 컬럼에 autoIncrement를 설정하면")
    class WhenAutoIncrementOnNonIntegerColumn {

      @Test
      @DisplayName("ColumnAutoIncrementNotAllowedException이 발생한다")
      void throwsColumnAutoIncrementNotAllowedException() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(ColumnAutoIncrementNotAllowedException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("INT 컬럼에 charset을 설정하면")
    class WhenCharsetOnIntColumn {

      @Test
      @DisplayName("ColumnCharsetNotAllowedException이 발생한다")
      void throwsColumnCharsetNotAllowedException() {
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(ColumnCharsetNotAllowedException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("이미 autoIncrement 컬럼이 있으면")
    class WhenAutoIncrementAlreadyExists {

      @Test
      @DisplayName("MultipleAutoIncrementColumnException이 발생한다")
      void throwsMultipleAutoIncrementColumnException() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);
        var column = ColumnFixture.intColumn();
        var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrementAndName(
            "01ARZ3NDEKTSV4RRFFQ69G5EXS", "existing_auto_increment");
        var columns = List.of(column, existingAutoIncrement);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(MultipleAutoIncrementColumnException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("PK 컬럼의 charset/collation을 변경하면")
    class WhenPkColumnCharsetChanged {

      @Test
      @DisplayName("FK 컬럼에도 charset/collation이 전파된다")
      void propagatesCharsetCollationToFkColumns() {
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", "utf8mb4_unicode_ci", null);
        var pkColumn = ColumnFixture.defaultColumn();
        var constraintId = "constraint-1";
        var relationshipId = "relationship-1";
        var fkColumnId = "fk-column-1";

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumn.id(), 0);
        var constraint = new Constraint(constraintId, pkColumn.tableId(), "pk_constraint",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relationship = new Relationship(relationshipId, pkColumn.tableId(), "fk-table-1",
            "rel_name", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumn.id(),
            fkColumnId, 0);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(pkColumn));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(pkColumn)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));
        given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId))
            .willReturn(Mono.just(List.of(relationshipColumn)));

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(pkColumn.id()), eq(false), eq("utf8mb4"),
                eq("utf8mb4_unicode_ci"), isNull());
        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(fkColumnId), isNull(), eq("utf8mb4"),
                eq("utf8mb4_unicode_ci"), isNull());
      }

    }

    @Nested
    @DisplayName("다단계 FK 체인에서 PK 컬럼의 charset을 변경하면")
    class WhenMultiLevelCascade {

      @Test
      @DisplayName("하위 FK 컬럼까지 재귀적으로 전파된다")
      void propagatesCharsetCollationRecursivelyToDownstreamFkColumns() {
        var colAId = ColumnFixture.DEFAULT_ID;
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", "utf8mb4_unicode_ci", null);

        var colA = new Column(colAId, "table-a", "pk_a", "VARCHAR",
            new ColumnLengthScale(255, null, null), 0, false, null, null, null);
        var colB = new Column("col-b", "table-b", "fk_b", "VARCHAR",
            new ColumnLengthScale(255, null, null), 0, false, null, null, null);
        var colC = new Column("col-c", "table-c", "fk_c", "VARCHAR",
            new ColumnLengthScale(255, null, null), 0, false, null, null, null);

        var ccA = new ConstraintColumn("cc-a", "cst-a", colAId, 0);
        var cstA = new Constraint("cst-a", "table-a", "pk_a",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relAB = new Relationship("rel-ab", "table-a", "table-b",
            "rel_ab", RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var rcAB = new RelationshipColumn("rc-ab", "rel-ab", colAId, "col-b", 0);

        var ccB = new ConstraintColumn("cc-b", "cst-b", "col-b", 0);
        var cstB = new Constraint("cst-b", "table-b", "pk_b",
            ConstraintKind.PRIMARY_KEY, null, null);
        var relBC = new Relationship("rel-bc", "table-b", "table-c",
            "rel_bc", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var rcBC = new RelationshipColumn("rc-bc", "rel-bc", "col-b", "col-c", 0);

        given(getColumnByIdPort.findColumnById(colAId)).willReturn(Mono.just(colA));
        given(getColumnByIdPort.findColumnById("col-b")).willReturn(Mono.just(colB));
        given(getColumnByIdPort.findColumnById("col-c")).willReturn(Mono.just(colC));
        given(getColumnsByTableIdPort.findColumnsByTableId("table-a"))
            .willReturn(Mono.just(List.of(colA)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
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

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(colAId), eq(false), eq("utf8mb4"),
                eq("utf8mb4_unicode_ci"), isNull());
        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq("col-b"), isNull(), eq("utf8mb4"),
                eq("utf8mb4_unicode_ci"), isNull());
        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq("col-c"), isNull(), eq("utf8mb4"),
                eq("utf8mb4_unicode_ci"), isNull());
      }

    }

    @Nested
    @DisplayName("비-PK 컬럼의 charset을 변경하면")
    class WhenNonPkColumnCharsetChanged {

      @Test
      @DisplayName("FK 전파가 발생하지 않는다")
      void doesNotPropagate() {
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", "utf8mb4_unicode_ci", null);
        var column = ColumnFixture.defaultColumn();
        var constraintId = "constraint-1";

        var constraintColumn = new ConstraintColumn("cc-1", constraintId, column.id(), 0);
        var constraint = new Constraint(constraintId, column.tableId(), "uq_constraint",
            ConstraintKind.UNIQUE, null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(column.id()))
            .willReturn(Mono.just(List.of(constraintColumn)));
        given(getConstraintByIdPort.findConstraintById(constraintId))
            .willReturn(Mono.just(constraint));

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should(times(1)).changeColumnMeta(any(), any(), any(), any(), any());
        then(getRelationshipsByPkTableIdPort).should(never()).findRelationshipsByPkTableId(any());
      }

    }

  }

}
