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

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
  PkCascadeHelper pkCascadeHelper;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  RemoveConstraintColumnService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("removeConstraintColumn 메서드는")
  class RemoveConstraintColumn {

    @Test
    @DisplayName("유효한 요청 시 컬럼을 삭제하고 남은 컬럼들을 재정렬한다")
    void removesColumnAndReordersRemaining() {
      var command = ConstraintFixture.removeColumnCommand("cc1");
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0);
      var remainingColumns = List.of(
          ConstraintFixture.constraintColumn("cc2", "constraint1", "col2", 1));

      given(getConstraintColumnByIdPort.findConstraintColumnById("cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(getConstraintByIdPort.findConstraintById("constraint1"))
          .willReturn(Mono.just(constraint));
      given(deleteConstraintColumnPort.deleteConstraintColumn("cc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("constraint1"))
          .willReturn(Mono.just(remainingColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteConstraintColumnPort).should().deleteConstraintColumn("cc1");
      then(changeConstraintColumnPositionPort).should()
          .changeConstraintColumnPositions(eq("constraint1"), any());
      then(deleteConstraintPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("마지막 컬럼 삭제 시 제약조건도 함께 삭제한다")
    void deletesConstraintWhenLastColumnRemoved() {
      var command = ConstraintFixture.removeColumnCommand("cc1");
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0);

      given(getConstraintColumnByIdPort.findConstraintColumnById("cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(getConstraintByIdPort.findConstraintById("constraint1"))
          .willReturn(Mono.just(constraint));
      given(deleteConstraintColumnPort.deleteConstraintColumn("cc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("constraint1"))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint("constraint1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteConstraintColumnPort).should().deleteConstraintColumn("cc1");
      then(deleteConstraintPort).should().deleteConstraint("constraint1");
      then(changeConstraintColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.removeColumnCommand("cc1");
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "nonexistent", "col1", 0);

      given(getConstraintColumnByIdPort.findConstraintColumnById("cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(getConstraintByIdPort.findConstraintById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NOT_FOUND))
          .verify();

      then(deleteConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintColumnNotExists() {
      var command = ConstraintFixture.removeColumnCommand("nonexistent");
      given(getConstraintColumnByIdPort.findConstraintColumnById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.COLUMN_NOT_FOUND))
          .verify();

      then(deleteConstraintColumnPort).shouldHaveNoInteractions();
    }

  }

  @Nested
  @DisplayName("PK Constraint 컬럼 제거 시")
  class WhenRemovingPkConstraintColumn {

    @Test
    @DisplayName("PkCascadeHelper를 통해 cascade 삭제를 수행한다")
    void cascadesRemovalViaPkCascadeHelper() {
      var command = ConstraintFixture.removeColumnCommand("pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);
      var remainingPkColumns = List.of(
          ConstraintFixture.constraintColumn("pk-cc2", "pk-constraint", "pk-col2", 1));

      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(pkCascadeHelper.cascadeRemovePkColumn(
          eq("pk-table"), eq("pk-col1"), anySet(), anySet()))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(remainingPkColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(pkCascadeHelper).should().cascadeRemovePkColumn(
          eq("pk-table"), eq("pk-col1"), anySet(), anySet());
    }

    @Test
    @DisplayName("마지막 PK 컬럼 삭제 시 제약조건도 함께 삭제한다")
    void deletesConstraintWhenLastPkColumnRemoved() {
      var command = ConstraintFixture.removeColumnCommand("pk-cc1");
      var pkConstraint = createConstraint("pk-constraint", "pk-table", ConstraintKind.PRIMARY_KEY);
      var pkConstraintColumn = ConstraintFixture.constraintColumn(
          "pk-cc1", "pk-constraint", "pk-col1", 0);

      given(getConstraintColumnByIdPort.findConstraintColumnById("pk-cc1"))
          .willReturn(Mono.just(pkConstraintColumn));
      given(getConstraintByIdPort.findConstraintById("pk-constraint"))
          .willReturn(Mono.just(pkConstraint));
      given(deleteConstraintColumnPort.deleteConstraintColumn("pk-cc1"))
          .willReturn(Mono.empty());
      given(pkCascadeHelper.cascadeRemovePkColumn(
          eq("pk-table"), eq("pk-col1"), anySet(), anySet()))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("pk-constraint"))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintPort.deleteConstraint("pk-constraint"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteConstraintPort).should().deleteConstraint("pk-constraint");
    }

  }

  @Nested
  @DisplayName("UNIQUE Constraint 컬럼 제거 시")
  class WhenRemovingUniqueConstraintColumn {

    @Test
    @DisplayName("PkCascadeHelper가 호출되지 않는다")
    void noRelationshipCascadeForUniqueConstraint() {
      var command = ConstraintFixture.removeColumnCommand("uq-cc1");
      var constraint = createConstraint("uq-constraint", "table1", ConstraintKind.UNIQUE);
      var constraintColumn = ConstraintFixture.constraintColumn(
          "uq-cc1", "uq-constraint", "col1", 0);
      var remainingColumns = List.of(
          ConstraintFixture.constraintColumn("uq-cc2", "uq-constraint", "col2", 1));

      given(getConstraintColumnByIdPort.findConstraintColumnById("uq-cc1"))
          .willReturn(Mono.just(constraintColumn));
      given(getConstraintByIdPort.findConstraintById("uq-constraint"))
          .willReturn(Mono.just(constraint));
      given(deleteConstraintColumnPort.deleteConstraintColumn("uq-cc1"))
          .willReturn(Mono.empty());
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId("uq-constraint"))
          .willReturn(Mono.just(remainingColumns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(anyString(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.removeConstraintColumn(command))
          .expectNextCount(1)
          .verifyComplete();

      then(pkCascadeHelper).shouldHaveNoInteractions();
    }

  }

  private Constraint createConstraint(String id, String tableId, ConstraintKind kind) {
    return new Constraint(id, tableId, "test_constraint", kind, null, null);
  }

}
