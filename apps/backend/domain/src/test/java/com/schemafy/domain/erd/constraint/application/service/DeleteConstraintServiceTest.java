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

import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteConstraintService")
class DeleteConstraintServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteConstraintPort deleteConstraintPort;

  @Mock
  DeleteConstraintColumnsByConstraintIdPort deleteConstraintColumnsPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  PkCascadeHelper pkCascadeHelper;

  @InjectMocks
  DeleteConstraintService sut;

  @BeforeEach
  void setUp() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("deleteConstraint 메서드는")
  class DeleteConstraint {

    @Test
    @DisplayName("UNIQUE 제약조건과 해당 컬럼들을 삭제한다")
    void deletesUniqueConstraintAndColumns() {
      var command = ConstraintFixture.deleteCommand();
      var constraint = new Constraint(
          ConstraintFixture.DEFAULT_ID, "table1", "uq_test", ConstraintKind.UNIQUE, null, null);

      given(getConstraintByIdPort.findConstraintById(ConstraintFixture.DEFAULT_ID))
          .willReturn(Mono.just(constraint));
      given(deleteConstraintColumnsPort.deleteByConstraintId(any()))
          .willReturn(Mono.empty());
      given(deleteConstraintPort.deleteConstraint(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteConstraint(command))
          .verifyComplete();

      var inOrderVerifier = inOrder(deleteConstraintColumnsPort, deleteConstraintPort);
      inOrderVerifier.verify(deleteConstraintColumnsPort).deleteByConstraintId(eq(ConstraintFixture.DEFAULT_ID));
      inOrderVerifier.verify(deleteConstraintPort).deleteConstraint(eq(ConstraintFixture.DEFAULT_ID));
      then(pkCascadeHelper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PK 제약조건 삭제 시 PkCascadeHelper를 통해 cascade 삭제를 수행한다")
    void cascadeDeletesViaPkCascadeHelperWhenPkDeleted() {
      var constraintId = "pk-constraint";
      var command = ConstraintFixture.deleteCommand(constraintId);
      var constraint = new Constraint(constraintId, "pk-table", "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumns = List.of(
          new ConstraintColumn("cc1", constraintId, "pk-col1", 0));

      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(constraintColumns));
      given(pkCascadeHelper.cascadeRemovePkColumn(eq("pk-table"), eq("pk-col1"), anySet()))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByConstraintId(constraintId))
          .willReturn(Mono.empty());
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteConstraint(command))
          .verifyComplete();

      then(pkCascadeHelper).should().cascadeRemovePkColumn(eq("pk-table"), eq("pk-col1"), anySet());
    }

    @Test
    @DisplayName("PK 제약조건에 여러 컬럼이 있을 때 모든 컬럼에 대해 cascade 삭제를 수행한다")
    void cascadeDeletesAllColumnsWhenPkHasMultipleColumns() {
      var constraintId = "pk-constraint";
      var command = ConstraintFixture.deleteCommand(constraintId);
      var constraint = new Constraint(constraintId, "pk-table", "pk_test", ConstraintKind.PRIMARY_KEY, null, null);
      var constraintColumns = List.of(
          new ConstraintColumn("cc1", constraintId, "pk-col1", 0),
          new ConstraintColumn("cc2", constraintId, "pk-col2", 1));

      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(constraintColumns));
      given(pkCascadeHelper.cascadeRemovePkColumn(eq("pk-table"), eq("pk-col1"), anySet()))
          .willReturn(Mono.empty());
      given(pkCascadeHelper.cascadeRemovePkColumn(eq("pk-table"), eq("pk-col2"), anySet()))
          .willReturn(Mono.empty());
      given(deleteConstraintColumnsPort.deleteByConstraintId(constraintId))
          .willReturn(Mono.empty());
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteConstraint(command))
          .verifyComplete();

      then(pkCascadeHelper).should().cascadeRemovePkColumn(eq("pk-table"), eq("pk-col1"), anySet());
      then(pkCascadeHelper).should().cascadeRemovePkColumn(eq("pk-table"), eq("pk-col2"), anySet());
    }

    @Test
    @DisplayName("PK 제약조건에 컬럼이 없으면 cascade 없이 삭제한다")
    void deletesWithoutCascadeWhenNoConstraintColumns() {
      var constraintId = "pk-constraint";
      var command = ConstraintFixture.deleteCommand(constraintId);
      var constraint = new Constraint(constraintId, "pk-table", "pk_test", ConstraintKind.PRIMARY_KEY, null, null);

      given(getConstraintByIdPort.findConstraintById(constraintId))
          .willReturn(Mono.just(constraint));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId))
          .willReturn(Mono.just(List.of()));
      given(deleteConstraintColumnsPort.deleteByConstraintId(constraintId))
          .willReturn(Mono.empty());
      given(deleteConstraintPort.deleteConstraint(constraintId))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteConstraint(command))
          .verifyComplete();

      then(pkCascadeHelper).should(never()).cascadeRemovePkColumn(any(), any(), any());
    }

  }

}
