package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveConstraintColumnService")
class RemoveConstraintColumnServiceTest {

  @Mock
  DeleteConstraintColumnPort deleteConstraintColumnPort;

  @Mock
  DeleteConstraintPort deleteConstraintPort;

  @Mock
  ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetConstraintColumnByIdPort getConstraintColumnByIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  DeleteRelationshipPort deleteRelationshipPort;

  @Mock
  DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort;

  @Mock
  DeleteRelationshipColumnPort deleteRelationshipColumnPort;

  @InjectMocks
  RemoveConstraintColumnService sut;

  @Nested
  @DisplayName("removeConstraintColumn 메서드는")
  class RemoveConstraintColumn {

    @Test
    @DisplayName("유효한 요청 시 컬럼을 삭제하고 남은 컬럼들을 재정렬한다")
    void removesColumnAndReordersRemaining() {
      var command = ConstraintFixture.removeColumnCommand("constraint1", "cc1");
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0);
      var remainingColumns = List.of(
          ConstraintFixture.constraintColumn("cc2", "constraint1", "col2", 1));

      given(getConstraintByIdPort.findConstraintById("constraint1"))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("cc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("constraint1"))
          .willReturn(Mono.just(remainingColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(deleteConstraintColumnPort).should().deleteConstraintColumn("cc1");
      then(changeConstraintColumnPositionPort).should()
          .changeConstraintColumnPositions(eq("constraint1"), any());
      then(deleteConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마지막 컬럼 삭제 시 제약조건도 함께 삭제한다")
    void deletesConstraintWhenLastColumnRemoved() {
      var command = ConstraintFixture.removeColumnCommand("constraint1", "cc1");
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0);

      given(getConstraintByIdPort.findConstraintById("constraint1"))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("cc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("constraint1"))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint("constraint1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(deleteConstraintColumnPort).should().deleteConstraintColumn("cc1");
      then(deleteConstraintPort).should().deleteConstraint("constraint1");
      then(changeConstraintColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.removeColumnCommand("nonexistent", "cc1");

      given(getConstraintByIdPort.findConstraintById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectError(ConstraintNotExistException.class)
          .verify();

      then(deleteConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintColumnNotExists() {
      var command = ConstraintFixture.removeColumnCommand("constraint1", "nonexistent");
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);

      given(getConstraintByIdPort.findConstraintById("constraint1"))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectError(ConstraintColumnNotExistException.class)
          .verify();

      then(deleteConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 해당 제약조건에 속하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotBelongToConstraint() {
      var command = ConstraintFixture.removeColumnCommand("constraint1", "cc1");
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "other-constraint", "col1", 0);

      given(getConstraintByIdPort.findConstraintById("constraint1"))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("cc1"))
          .willReturn(Mono.just(constraintColumn));

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectError(ConstraintColumnNotExistException.class)
          .verify();

      then(deleteConstraintColumnPort).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("PK Constraint 컬럼 제거 시")
  class WhenRemovingPkConstraintColumn {

    @Test
    @DisplayName("해당 pkColumnId를 참조하는 RelationshipColumn만 삭제한다")
    void deletesRelationshipColumnWhenPkColumnRemoved() {
      var command = ConstraintFixture.removeColumnCommand("pk-constraint", "pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);
      var remainingPkColumns = List.of(
          ConstraintFixture.constraintColumn("pk-cc2", "pk-constraint", "pk-col2", 1));

      // Relationship 관련 설정
      var relationship = RelationshipFixture.relationshipWithTables("pk-table", "fk-table");
      var relationshipColumns = List.of(
          RelationshipFixture.relationshipColumn("rc1", relationship.id(), "pk-col1", "fk-col1", 0),
          RelationshipFixture.relationshipColumn("rc2", relationship.id(), "pk-col2", "fk-col2", 1));

      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("pk-table"))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId(relationship.id()))
          .willReturn(Mono.just(relationshipColumns));
      given(deleteRelationshipColumnPort.deleteRelationshipColumn("rc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(remainingPkColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(deleteRelationshipColumnPort).should().deleteRelationshipColumn("rc1");
      then(deleteRelationshipPort).should(never()).deleteRelationship(any());
    }

    @Test
    @DisplayName("마지막 RelationshipColumn이면 Relationship 자체도 삭제한다")
    void deletesRelationshipWhenLastRelationshipColumnRemoved() {
      var command = ConstraintFixture.removeColumnCommand("pk-constraint", "pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);

      // Relationship에 하나의 컬럼만 있는 경우
      var relationship = RelationshipFixture.relationshipWithTables("pk-table", "fk-table");
      var relationshipColumns = List.of(
          RelationshipFixture.relationshipColumn("rc1", relationship.id(), "pk-col1", "fk-col1", 0));

      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("pk-table"))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId(relationship.id()))
          .willReturn(Mono.just(relationshipColumns));
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId(relationship.id()))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(relationship.id()))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint("pk-constraint"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(deleteRelationshipColumnsByRelationshipIdPort).should().deleteByRelationshipId(relationship.id());
      then(deleteRelationshipPort).should().deleteRelationship(relationship.id());
    }

    @Test
    @DisplayName("PK 테이블을 참조하는 Relationship이 없으면 연쇄 삭제가 발생하지 않는다")
    void noRelationshipCascadeWhenNoRelationshipsExist() {
      var command = ConstraintFixture.removeColumnCommand("pk-constraint", "pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);
      var remainingPkColumns = List.of(
          ConstraintFixture.constraintColumn("pk-cc2", "pk-constraint", "pk-col2", 1));

      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("pk-table"))
          .willReturn(Mono.just(List.of()));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(remainingPkColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(getRelationshipColumnsByRelationshipIdPort).shouldHaveNoInteractions();
      then(deleteRelationshipColumnPort).shouldHaveNoInteractions();
      then(deleteRelationshipPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("RelationshipColumn 중 해당 pkColumnId를 참조하는 것이 없으면 삭제하지 않는다")
    void noDeleteWhenNoMatchingPkColumnId() {
      var command = ConstraintFixture.removeColumnCommand("pk-constraint", "pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);
      var remainingPkColumns = List.of(
          ConstraintFixture.constraintColumn("pk-cc2", "pk-constraint", "pk-col2", 1));

      // Relationship은 있지만 다른 pkColumnId를 참조
      var relationship = RelationshipFixture.relationshipWithTables("pk-table", "fk-table");
      var relationshipColumns = List.of(
          RelationshipFixture.relationshipColumn("rc1", relationship.id(), "pk-col3", "fk-col3", 0));

      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("pk-table"))
          .willReturn(Mono.just(List.of(relationship)));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId(relationship.id()))
          .willReturn(Mono.just(relationshipColumns));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(remainingPkColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(deleteRelationshipColumnPort).shouldHaveNoInteractions();
      then(deleteRelationshipPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("여러 Relationship에서 해당 pkColumnId를 참조하는 컬럼들을 모두 삭제한다")
    void deletesFromMultipleRelationships() {
      var command = ConstraintFixture.removeColumnCommand("pk-constraint", "pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);
      var remainingPkColumns = List.of(
          ConstraintFixture.constraintColumn("pk-cc2", "pk-constraint", "pk-col2", 1));

      // 두 개의 Relationship이 같은 PK 테이블을 참조
      var relationship1 = new com.schemafy.domain.erd.relationship.domain.Relationship(
          "rel1", "pk-table", "fk-table1", "fk_rel1",
          com.schemafy.domain.erd.relationship.domain.type.RelationshipKind.NON_IDENTIFYING,
          com.schemafy.domain.erd.relationship.domain.type.Cardinality.ONE_TO_MANY, null);
      var relationship2 = new com.schemafy.domain.erd.relationship.domain.Relationship(
          "rel2", "pk-table", "fk-table2", "fk_rel2",
          com.schemafy.domain.erd.relationship.domain.type.RelationshipKind.NON_IDENTIFYING,
          com.schemafy.domain.erd.relationship.domain.type.Cardinality.ONE_TO_MANY, null);

      var rel1Columns = List.of(
          RelationshipFixture.relationshipColumn("rc1", "rel1", "pk-col1", "fk-col1", 0),
          RelationshipFixture.relationshipColumn("rc2", "rel1", "pk-col2", "fk-col2", 1));
      var rel2Columns = List.of(
          RelationshipFixture.relationshipColumn("rc3", "rel2", "pk-col1", "fk-col3", 0));

      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId("pk-table"))
          .willReturn(Mono.just(List.of(relationship1, relationship2)));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId("rel1"))
          .willReturn(Mono.just(rel1Columns));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId("rel2"))
          .willReturn(Mono.just(rel2Columns));
      given(deleteRelationshipColumnPort.deleteRelationshipColumn("rc1"))
          .willReturn(Mono.empty());
      // rel2는 마지막 컬럼이므로 Relationship도 삭제
      given(deleteRelationshipColumnsByRelationshipIdPort.deleteByRelationshipId("rel2"))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship("rel2"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(remainingPkColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(deleteRelationshipColumnPort).should().deleteRelationshipColumn("rc1");
      then(deleteRelationshipPort).should().deleteRelationship("rel2");
    }
  }

  @Nested
  @DisplayName("UNIQUE Constraint 컬럼 제거 시")
  class WhenRemovingUniqueConstraintColumn {

    @Test
    @DisplayName("RelationshipColumn 연쇄 삭제가 발생하지 않는다")
    void noRelationshipCascadeForUniqueConstraint() {
      var command = ConstraintFixture.removeColumnCommand("uq-constraint", "uq-cc1");
      var constraint = createConstraint("uq-constraint", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn(
          "uq-cc1", "uq-constraint", "col1", 0);
      var remainingColumns = List.of(
          ConstraintFixture.constraintColumn("uq-cc2", "uq-constraint", "col2", 1));

      given(getConstraintByIdPort.findConstraintById("uq-constraint"))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnByIdPort.findConstraintColumnById("uq-cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(deleteConstraintColumnPort.deleteConstraintColumn("uq-cc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("uq-constraint"))
          .willReturn(Mono.just(remainingColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .verifyComplete();

      then(getRelationshipsByPkTableIdPort).shouldHaveNoInteractions();
      then(getRelationshipColumnsByRelationshipIdPort).shouldHaveNoInteractions();
      then(deleteRelationshipColumnPort).shouldHaveNoInteractions();
      then(deleteRelationshipPort).shouldHaveNoInteractions();
    }
  }

  private Constraint createConstraint(String id, String tableId, ConstraintKind kind) {
    return new Constraint(id, tableId, "test_constraint", kind, null, null);
  }

}
