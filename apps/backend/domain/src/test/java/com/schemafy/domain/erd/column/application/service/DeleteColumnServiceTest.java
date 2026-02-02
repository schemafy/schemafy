package com.schemafy.domain.erd.column.application.service;

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

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnPort;
import com.schemafy.domain.erd.column.domain.exception.ForeignKeyColumnProtectedException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteColumnService")
class DeleteColumnServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteColumnPort deleteColumnPort;

  @Mock
  GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;
  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  @Mock
  DeleteConstraintColumnsByColumnIdPort deleteConstraintColumnsPort;
  @Mock
  DeleteConstraintPort deleteConstraintPort;
  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetIndexColumnsByColumnIdPort getIndexColumnsByColumnIdPort;
  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  @Mock
  DeleteIndexColumnsByColumnIdPort deleteIndexColumnsPort;
  @Mock
  DeleteIndexPort deleteIndexPort;

  @Mock
  GetRelationshipColumnsByColumnIdPort getRelationshipColumnsByColumnIdPort;
  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  @Mock
  DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  @Mock
  DeleteRelationshipColumnsByColumnIdPort deleteRelationshipColumnsPort;
  @Mock
  DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort;
  @Mock
  DeleteRelationshipPort deleteRelationshipPort;
  @Mock
  GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @InjectMocks
  DeleteColumnService sut;

  @BeforeEach
  void setUp() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("deleteColumn 메서드는")
  class DeleteColumn {

    @Test
    @DisplayName("관련 엔티티가 없으면 컬럼만 삭제한다")
    void deletesOnlyColumnWhenNoRelatedEntities() {
      var command = ColumnFixture.deleteCommand();

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteColumnPort).should().deleteColumn(command.columnId());
      then(deleteConstraintColumnsPort).should(never()).deleteByColumnId(any());
      then(deleteIndexColumnsPort).should(never()).deleteByColumnId(any());
      then(deleteRelationshipColumnsPort).should(never()).deleteByColumnId(any());
    }

    @Test
    @DisplayName("마지막 constraint column이 삭제되면 constraint도 삭제한다")
    void deletesOrphanConstraintWhenLastColumnDeleted() {
      var command = ColumnFixture.deleteCommand();
      String constraintId = "constraint-1";
      var constraintColumn = new ConstraintColumn("cc-1", constraintId, command.columnId(), 0);
      var constraint = new Constraint(constraintId, "table-1", "uk_test", ConstraintKind.UNIQUE, null, null);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(command.columnId()))
          .willReturn(Mono.just(List.of(constraintColumn)));
      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(constraint));
      given(deleteConstraintColumnsPort.deleteByColumnId(command.columnId()))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of())); // 삭제 후 빈 목록
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteConstraintColumnsPort).should().deleteByColumnId(command.columnId());
      then(deleteConstraintPort).should().deleteConstraint(constraintId);
    }

    @Test
    @DisplayName("마지막 index column이 삭제되면 index도 삭제한다")
    void deletesOrphanIndexWhenLastColumnDeleted() {
      var command = ColumnFixture.deleteCommand();
      String indexId = "index-1";
      var indexColumn = new IndexColumn("ic-1", indexId, command.columnId(), 0, SortDirection.ASC);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(command.columnId()))
          .willReturn(Mono.just(List.of(indexColumn)));
      given(deleteIndexColumnsPort.deleteByColumnId(command.columnId()))
          .willReturn(Mono.empty());
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(indexId))
          .willReturn(Mono.just(List.of())); // 삭제 후 빈 목록
      given(deleteIndexPort.deleteIndex(indexId))
          .willReturn(Mono.empty());

      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(any()))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteIndexColumnsPort).should().deleteByColumnId(command.columnId());
      then(deleteIndexPort).should().deleteIndex(indexId);
    }

    @Test
    @DisplayName("FK 컬럼을 직접 삭제하면 ForeignKeyColumnProtectedException이 발생한다")
    void rejectsDeletionOfForeignKeyColumn() {
      var command = ColumnFixture.deleteCommand();
      String relationshipId = "rel-1";
      var relationshipColumn = new RelationshipColumn(
          "rc-1", relationshipId, "pk-col", command.columnId(), 0);

      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(command.columnId()))
          .willReturn(Mono.just(List.of(relationshipColumn)));

      StepVerifier.create(sut.deleteColumn(command))
          .expectError(ForeignKeyColumnProtectedException.class)
          .verify();

      then(deleteColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 컬럼이 남아있으면 constraint/index/relationship을 삭제하지 않는다")
    void doesNotDeleteParentWhenOtherColumnsRemain() {
      var command = ColumnFixture.deleteCommand();
      String constraintId = "constraint-1";
      String indexId = "index-1";
      String relationshipId = "rel-1";

      var constraintColumn = new ConstraintColumn("cc-1", constraintId, command.columnId(), 0);
      var remainingConstraintColumn = new ConstraintColumn("cc-2", constraintId, "other-col", 1);
      var constraint = new Constraint(constraintId, "table-1", "uk_test", ConstraintKind.UNIQUE, null, null);

      var indexColumn = new IndexColumn("ic-1", indexId, command.columnId(), 0, SortDirection.ASC);
      var remainingIndexColumn = new IndexColumn("ic-2", indexId, "other-col", 1, SortDirection.ASC);

      // 컬럼이 PK 쪽에 있는 relationship (FK가 아니므로 삭제 가능)
      var relationshipColumn = new RelationshipColumn(
          "rc-1", relationshipId, command.columnId(), "fk-col", 0);
      var remainingRelationshipColumn = new RelationshipColumn(
          "rc-2", relationshipId, "pk-col-2", "other-col", 1);

      // FK 컬럼이 아니므로 보호 검증 통과
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(command.columnId()))
          .willReturn(Mono.just(List.of(relationshipColumn)));

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(command.columnId()))
          .willReturn(Mono.just(List.of(constraintColumn)));
      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(constraint));
      given(deleteConstraintColumnsPort.deleteByColumnId(command.columnId()))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of(remainingConstraintColumn)));

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(command.columnId()))
          .willReturn(Mono.just(List.of(indexColumn)));
      given(deleteIndexColumnsPort.deleteByColumnId(command.columnId()))
          .willReturn(Mono.empty());
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(indexId))
          .willReturn(Mono.just(List.of(remainingIndexColumn)));

      given(deleteRelationshipColumnsPort.deleteByColumnId(command.columnId()))
          .willReturn(Mono.empty());
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId))
          .willReturn(Mono.just(List.of(remainingRelationshipColumn)));

      given(deleteColumnPort.deleteColumn(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteConstraintPort).should(never()).deleteConstraint(any());
      then(deleteIndexPort).should(never()).deleteIndex(any());
      then(deleteRelationshipPort).should(never()).deleteRelationship(any());
    }

    @Test
    @DisplayName("PK 컬럼 삭제 시 FK 컬럼도 cascade 삭제한다")
    void cascadeDeletesFkColumnWhenPkColumnDeleted() {
      String pkColumnId = "pk-col";
      String fkColumnId = "fk-col";
      String tableId = "table-1";
      String fkTableId = "table-2";
      String constraintId = "pk-constraint-1";
      String relationshipId = "rel-1";

      var command = ColumnFixture.deleteCommand(pkColumnId);
      var pkConstraint = new Constraint(constraintId, tableId, "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumnId, 0);
      var relationship = new Relationship(
          relationshipId, tableId, fkTableId, "fk_test", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
          null);
      var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumnId, fkColumnId, 0);

      // PK 컬럼에 대한 설정
      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of(constraintColumn)));
      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(pkConstraint));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableId))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId))
          .willReturn(Mono.just(List.of(relationshipColumn)));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relationshipId))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(relationshipId))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(pkColumnId))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of())); // 삭제 후 빈 목록
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      // FK 컬럼에 대한 설정 (cascade로 호출됨)
      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
          .willReturn(Mono.just(List.of()));
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(fkColumnId))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(fkColumnId))
          .willReturn(Mono.just(List.of())); // RelationshipColumn은 이미 삭제됨
      given(deleteColumnPort.deleteColumn(fkColumnId))
          .willReturn(Mono.empty());

      // PK 컬럼에 대한 나머지 설정
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(pkColumnId))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      // FK 컬럼이 삭제되었는지 확인
      then(deleteColumnPort).should().deleteColumn(fkColumnId);
      then(deleteColumnPort).should().deleteColumn(pkColumnId);
    }

    @Test
    @DisplayName("다중 FK 컬럼을 cascade 삭제한다")
    void cascadeDeletesMultipleFkColumns() {
      String pkColumnId = "pk-col";
      String fkColumnId1 = "fk-col-1";
      String fkColumnId2 = "fk-col-2";
      String tableId = "table-1";
      String fkTableId1 = "table-2";
      String fkTableId2 = "table-3";
      String constraintId = "pk-constraint-1";
      String relationshipId1 = "rel-1";
      String relationshipId2 = "rel-2";

      var command = ColumnFixture.deleteCommand(pkColumnId);
      var pkConstraint = new Constraint(constraintId, tableId, "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumnId, 0);
      var relationship1 = new Relationship(
          relationshipId1, tableId, fkTableId1, "fk_test1", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
          null);
      var relationship2 = new Relationship(
          relationshipId2, tableId, fkTableId2, "fk_test2", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
          null);
      var relationshipColumn1 = new RelationshipColumn("rc-1", relationshipId1, pkColumnId, fkColumnId1, 0);
      var relationshipColumn2 = new RelationshipColumn("rc-2", relationshipId2, pkColumnId, fkColumnId2, 0);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of(constraintColumn)));
      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(pkConstraint));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableId))
          .willReturn(Mono.just(List.of(relationship1, relationship2)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId1))
          .willReturn(Mono.just(List.of(relationshipColumn1)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId2))
          .willReturn(Mono.just(List.of(relationshipColumn2)));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(any()))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(any()))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(pkColumnId))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      // FK 컬럼들에 대한 설정
      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(fkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(fkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(fkColumnId1))
          .willReturn(Mono.empty());

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId2))
          .willReturn(Mono.just(List.of()));
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(fkColumnId2))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(fkColumnId2))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(fkColumnId2))
          .willReturn(Mono.empty());

      // PK 컬럼에 대한 나머지 설정
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(pkColumnId))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteColumnPort).should().deleteColumn(fkColumnId1);
      then(deleteColumnPort).should().deleteColumn(fkColumnId2);
      then(deleteColumnPort).should().deleteColumn(pkColumnId);
    }

    @Test
    @DisplayName("remaining이 남아있을 때 RelationshipColumn만 삭제한다")
    void deletesOnlyRelationshipColumnWhenRemainingExists() {
      String pkColumnId1 = "pk-col-1";
      String pkColumnId2 = "pk-col-2";
      String fkColumnId1 = "fk-col-1";
      String tableId = "table-1";
      String fkTableId = "table-2";
      String constraintId = "pk-constraint-1";
      String relationshipId = "rel-1";

      var command = ColumnFixture.deleteCommand(pkColumnId1);
      var pkConstraint = new Constraint(constraintId, tableId, "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumn = new ConstraintColumn("cc-1", constraintId, pkColumnId1, 0);
      var relationship = new Relationship(
          relationshipId, tableId, fkTableId, "fk_test", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
          null);
      var relationshipColumn1 = new RelationshipColumn("rc-1", relationshipId, pkColumnId1, fkColumnId1, 0);
      var relationshipColumn2 = new RelationshipColumn("rc-2", relationshipId, pkColumnId2, "fk-col-2", 1);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumnId1))
          .willReturn(Mono.just(List.of(constraintColumn)));
      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(pkConstraint));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableId))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId))
          .willReturn(Mono.just(List.of(relationshipColumn1, relationshipColumn2)));
      given(deleteRelationshipColumnPort.deleteRelationshipColumn("rc-1"))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(pkColumnId1))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of())); // 삭제 후 빈 목록
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      // FK 컬럼에 대한 설정 (cascade로 호출됨)
      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(fkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(fkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(fkColumnId1))
          .willReturn(Mono.empty());

      // PK 컬럼에 대한 나머지 설정
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(pkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(pkColumnId1))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(pkColumnId1))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      // RelationshipColumn만 삭제되고, Relationship은 삭제되지 않음
      then(deleteRelationshipColumnPort).should().deleteRelationshipColumn("rc-1");
      then(deleteRelationshipPort).should(never()).deleteRelationship(any());
      then(deleteColumnPort).should().deleteColumn(fkColumnId1);
    }

    @Test
    @DisplayName("순환 참조 시 무한 루프를 방지한다")
    void preventsInfiniteLoopOnCircularReference() {
      String columnId = "col-1";

      var command = ColumnFixture.deleteCommand(columnId);
      String tableId = "table-1";
      String constraintId = "pk-constraint-1";
      String relationshipId = "rel-1";

      var pkConstraint = new Constraint(constraintId, tableId, "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumn = new ConstraintColumn("cc-1", constraintId, columnId, 0);
      var relationship = new Relationship(
          relationshipId, tableId, tableId, "self_ref", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE, null);
      // 자기 자신을 참조하는 RelationshipColumn (순환 참조)
      var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, columnId, columnId, 0);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(columnId))
          .willReturn(Mono.just(List.of(constraintColumn)));
      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(pkConstraint));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableId))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId))
          .willReturn(Mono.just(List.of(relationshipColumn)));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relationshipId))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(relationshipId))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(columnId))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(columnId))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(columnId))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(columnId))
          .willReturn(Mono.empty());

      // 무한 루프 없이 완료되어야 함
      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      // 컬럼은 정확히 한 번만 삭제됨
      then(deleteColumnPort).should(times(1)).deleteColumn(columnId);
    }

    @Test
    @DisplayName("FK 컬럼에 연결된 Constraint와 Index도 cascade 삭제한다")
    void cascadeDeletesFkColumnWithConstraintAndIndex() {
      String pkColumnId = "pk-col";
      String fkColumnId = "fk-col";
      String tableId = "table-1";
      String fkTableId = "table-2";
      String pkConstraintId = "pk-constraint-1";
      String fkConstraintId = "fk-constraint-1";
      String fkIndexId = "fk-index-1";
      String relationshipId = "rel-1";

      var command = ColumnFixture.deleteCommand(pkColumnId);
      var pkConstraint = new Constraint(pkConstraintId, tableId, "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var pkConstraintColumn = new ConstraintColumn("cc-1", pkConstraintId, pkColumnId, 0);
      var relationship = new Relationship(
          relationshipId, tableId, fkTableId, "fk_test", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE,
          null);
      var relationshipColumn = new RelationshipColumn("rc-1", relationshipId, pkColumnId, fkColumnId, 0);

      // FK 컬럼에 연결된 Constraint와 Index
      var fkConstraint = new Constraint(fkConstraintId, fkTableId, "uk_fk", ConstraintKind.UNIQUE, null, null);
      var fkConstraintColumn = new ConstraintColumn("cc-2", fkConstraintId, fkColumnId, 0);
      var fkIndexColumn = new IndexColumn("ic-1", fkIndexId, fkColumnId, 0, SortDirection.ASC);

      // PK 컬럼 관련 설정
      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of(pkConstraintColumn)));
      given(getConstraintByIdPort.findConstraintById(pkConstraintId))
          .willReturn(Mono.just(pkConstraint));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableId))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId))
          .willReturn(Mono.just(List.of(relationshipColumn)));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relationshipId))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(relationshipId))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(pkColumnId))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(pkConstraintId))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint(pkConstraintId))
          .willReturn(Mono.empty());
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(pkColumnId))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(pkColumnId))
          .willReturn(Mono.empty());

      // FK 컬럼 관련 설정 (Constraint와 Index가 있음)
      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(fkColumnId))
          .willReturn(Mono.just(List.of(fkConstraintColumn)));
      given(getConstraintByIdPort.findConstraintById(fkConstraintId))
          .willReturn(Mono.just(fkConstraint));
      given(deleteConstraintColumnsPort.deleteByColumnId(fkColumnId))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(fkConstraintId))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint(fkConstraintId))
          .willReturn(Mono.empty());

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(fkColumnId))
          .willReturn(Mono.just(List.of(fkIndexColumn)));
      given(deleteIndexColumnsPort.deleteByColumnId(fkColumnId))
          .willReturn(Mono.empty());
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(fkIndexId))
          .willReturn(Mono.just(List.of()));
      given(deleteIndexPort.deleteIndex(fkIndexId))
          .willReturn(Mono.empty());

      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(fkColumnId))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(fkColumnId))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      // FK 컬럼의 Constraint와 Index도 삭제되었는지 확인
      then(deleteConstraintPort).should().deleteConstraint(fkConstraintId);
      then(deleteIndexPort).should().deleteIndex(fkIndexId);
      then(deleteColumnPort).should().deleteColumn(fkColumnId);
      then(deleteColumnPort).should().deleteColumn(pkColumnId);
    }

    @Test
    @DisplayName("체인 cascade 삭제: A→B→C 관계에서 A 삭제 시 B, C 모두 삭제")
    void cascadeDeletesChainedFkColumns() {
      // A(PK) → B(FK/PK) → C(FK)
      String colA = "col-a";
      String colB = "col-b";
      String colC = "col-c";
      String tableA = "table-a";
      String tableB = "table-b";
      String tableC = "table-c";
      String constraintA = "pk-a";
      String constraintB = "pk-b";
      String relAB = "rel-ab";
      String relBC = "rel-bc";

      var command = ColumnFixture.deleteCommand(colA);

      // 테이블 A: colA가 PK
      var pkConstraintA = new Constraint(constraintA, tableA, "pk_a", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumnA = new ConstraintColumn("cc-a", constraintA, colA, 0);

      // 테이블 B: colB가 PK이자 FK(A 참조)
      var pkConstraintB = new Constraint(constraintB, tableB, "pk_b", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumnB = new ConstraintColumn("cc-b", constraintB, colB, 0);

      // Relationship A → B
      var relationshipAB = new Relationship(
          relAB, tableA, tableB, "fk_ab", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE, null);
      var relationshipColumnAB = new RelationshipColumn("rc-ab", relAB, colA, colB, 0);

      // Relationship B → C
      var relationshipBC = new Relationship(
          relBC, tableB, tableC, "fk_bc", RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_ONE, null);
      var relationshipColumnBC = new RelationshipColumn("rc-bc", relBC, colB, colC, 0);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(colA))
          .willReturn(Mono.just(List.of(constraintColumnA)));
      given(getConstraintByIdPort.findConstraintById(constraintA))
          .willReturn(Mono.just(pkConstraintA));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableA))
          .willReturn(Mono.just(List.of(relationshipAB)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relAB))
          .willReturn(Mono.just(List.of(relationshipColumnAB)));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relAB))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(relAB))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(colA))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintA))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint(constraintA))
          .willReturn(Mono.empty());
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(colA))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(colA))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(colA))
          .willReturn(Mono.empty());

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(colB))
          .willReturn(Mono.just(List.of(constraintColumnB)));
      given(getConstraintByIdPort.findConstraintById(constraintB))
          .willReturn(Mono.just(pkConstraintB));
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableB))
          .willReturn(Mono.just(List.of(relationshipBC)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relBC))
          .willReturn(Mono.just(List.of(relationshipColumnBC)));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relBC))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(relBC))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByColumnId(colB))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintB))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint(constraintB))
          .willReturn(Mono.empty());
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(colB))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(colB))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(colB))
          .willReturn(Mono.empty());

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(colC))
          .willReturn(Mono.just(List.of()));
      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(colC))
          .willReturn(Mono.just(List.of()));
      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(colC))
          .willReturn(Mono.just(List.of()));
      given(deleteColumnPort.deleteColumn(colC))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      // A, B, C 모두 삭제되었는지 확인
      then(deleteColumnPort).should(times(1)).deleteColumn(colA);
      then(deleteColumnPort).should(times(1)).deleteColumn(colB);
      then(deleteColumnPort).should(times(1)).deleteColumn(colC);
    }

    @Test
    @DisplayName("Identifying 관계에서 PK 컬럼 삭제 시 FK 테이블의 컬럼, Relationship, Constraint 모두 cascade 삭제")
    void cascadeDeletesIdentifyingRelationshipWithAllRelatedEntities() {
      // 테이블 A의 컬럼 B (PK) → 테이블 C의 컬럼 D (FK) - Identifying 관계
      // D 컬럼은 C 테이블의 PK 일부이기도 함 (Identifying 관계의 특성)
      String colB = "col-b";  // A 테이블의 PK 컬럼
      String colD = "col-d";  // C 테이블의 FK 컬럼 (동시에 C의 PK 일부)
      String tableA = "table-a";
      String tableC = "table-c";
      String pkConstraintIdA = "pk-constraint-a";  // A 테이블의 PK constraint
      String pkConstraintIdC = "pk-constraint-c";  // C 테이블의 PK constraint (D 포함)
      String relationshipId = "rel-1";
      String relationshipColumnId = "rc-1";
      String pkConstraintColumnIdA = "cc-pk-a";
      String pkConstraintColumnIdC = "cc-pk-c";

      var command = ColumnFixture.deleteCommand(colB);

      // A 테이블의 PK Constraint
      var pkConstraintA = new Constraint(
          pkConstraintIdA, tableA, "pk_a", ConstraintKind.PRIMARY_KEY, null, null);
      var pkConstraintColumnA = new ConstraintColumn(pkConstraintColumnIdA, pkConstraintIdA, colB, 0);

      // C 테이블의 PK Constraint (Identifying 관계이므로 FK인 D도 PK의 일부)
      var pkConstraintC = new Constraint(
          pkConstraintIdC, tableC, "pk_c", ConstraintKind.PRIMARY_KEY, null, null);
      var pkConstraintColumnC = new ConstraintColumn(pkConstraintColumnIdC, pkConstraintIdC, colD, 0);

      // Identifying Relationship (A → C)
      var relationship = new Relationship(
          relationshipId, tableA, tableC, "fk_a_to_c",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
      var relationshipColumn = new RelationshipColumn(
          relationshipColumnId, relationshipId, colB, colD, 0);

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(colB))
          .willReturn(Mono.just(List.of(pkConstraintColumnA)));

      given(getConstraintByIdPort.findConstraintById(pkConstraintIdA))
          .willReturn(Mono.just(pkConstraintA));

      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableA))
          .willReturn(Mono.just(List.of(relationship)));

      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(relationshipId))
          .willReturn(Mono.just(List.of(relationshipColumn)));

      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relationshipId))
          .willReturn(Mono.empty());

      given(deleteRelationshipPort.deleteRelationship(relationshipId))
          .willReturn(Mono.empty());

      given(deleteConstraintColumnsPort.deleteByColumnId(colB))
          .willReturn(Mono.empty());

      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(pkConstraintIdA))
          .willReturn(Mono.just(List.of()));

      given(deleteConstraintPort.deleteConstraint(pkConstraintIdA))
          .willReturn(Mono.empty());

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(colB))
          .willReturn(Mono.just(List.of()));

      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(colB))
          .willReturn(Mono.just(List.of()));

      given(deleteColumnPort.deleteColumn(colB))
          .willReturn(Mono.empty());

      given(getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(colD))
          .willReturn(Mono.just(List.of(pkConstraintColumnC)));

      given(getConstraintByIdPort.findConstraintById(pkConstraintIdC))
          .willReturn(Mono.just(pkConstraintC));

      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(tableC))
          .willReturn(Mono.just(List.of()));

      given(deleteConstraintColumnsPort.deleteByColumnId(colD))
          .willReturn(Mono.empty());

      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(pkConstraintIdC))
          .willReturn(Mono.just(List.of()));

      given(deleteConstraintPort.deleteConstraint(pkConstraintIdC))
          .willReturn(Mono.empty());

      given(getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(colD))
          .willReturn(Mono.just(List.of()));

      given(getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(colD))
          .willReturn(Mono.just(List.of()));

      given(deleteColumnPort.deleteColumn(colD))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteColumn(command))
          .verifyComplete();

      then(deleteColumnPort).should(times(1)).deleteColumn(colD);

      then(deleteRelationshipColumnsByRelationshipIdPort).should(times(1))
          .deleteByRelationshipId(relationshipId);

      then(deleteRelationshipPort).should(times(1)).deleteRelationship(relationshipId);

      then(deleteConstraintPort).should(times(1)).deleteConstraint(pkConstraintIdC);

      then(deleteConstraintColumnsPort).should(times(1)).deleteByColumnId(colD);

      then(deleteColumnPort).should(times(1)).deleteColumn(colB);

      then(deleteConstraintPort).should(times(1)).deleteConstraint(pkConstraintIdA);
    }

  }

}
