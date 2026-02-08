package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintExpressionPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintDefinitionDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeConstraintExpressionService")
class ChangeConstraintExpressionServiceTest {

  @Mock
  ChangeConstraintExpressionPort changeConstraintExpressionPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @InjectMocks
  ChangeConstraintExpressionService sut;

  @Nested
  @DisplayName("changeConstraintCheckExpr 메서드는")
  class ChangeConstraintCheckExpr {

    @Test
    @DisplayName("CHECK 제약조건의 표현식을 변경한다")
    void changesCheckExprWhenKindIsCheck() {
      var constraint = ConstraintFixture.checkConstraintWithExpr("column1 > 0");
      var command = ConstraintFixture.changeCheckExprCommand(constraint.id(), "  column1 > 10  ");
      var columns = List.of(ConstraintFixture.constraintColumn("cc1", constraint.id(), "col1", 0));

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(constraint.tableId()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraint.id()))
          .willReturn(Mono.just(columns));
      given(changeConstraintExpressionPort.changeConstraintExpressions(
          constraint.id(), "column1 > 10", null))
              .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintCheckExpr(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeConstraintExpressionPort).should()
          .changeConstraintExpressions(eq(constraint.id()), eq("column1 > 10"), eq((String) null));
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.changeCheckExprCommand("non-existent-id", "column1 > 10");

      given(getConstraintByIdPort.findConstraintById("non-existent-id"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintCheckExpr(command))
          .expectError(ConstraintNotExistException.class)
          .verify();

      then(changeConstraintExpressionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("CHECK 이외의 kind에서는 예외가 발생한다")
    void throwsWhenKindMismatch() {
      var constraint = ConstraintFixture.defaultConstraintValue();
      var command = ConstraintFixture.changeCheckExprCommand(constraint.id(), "column1 > 10");

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));

      StepVerifier.create(sut.changeConstraintCheckExpr(command))
          .expectError(InvalidValueException.class)
          .verify();

      then(changeConstraintExpressionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 정의의 CHECK 제약조건이 이미 존재하면 예외가 발생한다")
    void throwsWhenDuplicateDefinition() {
      var constraint = ConstraintFixture.checkConstraintWithExpr("column1 > 0");
      var existingConstraint = ConstraintFixture.constraint(
          "01ARZ3NDEKTSV4RRFFQ69G5AN1",
          constraint.tableId(),
          "ck_duplicate",
          ConstraintKind.CHECK,
          "column1 > 10",
          null);
      var command = ConstraintFixture.changeCheckExprCommand(constraint.id(), "column1 > 10");
      var currentColumns = List.of(ConstraintFixture.constraintColumn("cc1", constraint.id(), "col1", 0));
      var existingColumns = List.of(ConstraintFixture.constraintColumn("cc2", existingConstraint.id(), "col1", 0));

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(constraint.tableId()))
          .willReturn(Mono.just(List.of(constraint, existingConstraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willAnswer(invocation -> {
            String constraintId = invocation.getArgument(0);
            if (constraint.id().equals(constraintId)) {
              return Mono.just(currentColumns);
            }
            if (existingConstraint.id().equals(constraintId)) {
              return Mono.just(existingColumns);
            }
            return Mono.just(List.of());
          });

      StepVerifier.create(sut.changeConstraintCheckExpr(command))
          .expectError(ConstraintDefinitionDuplicateException.class)
          .verify();

      then(changeConstraintExpressionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("표현식이 공백이면 CHECK 표현식을 제거한다")
    void clearsCheckExprWhenBlank() {
      var constraint = ConstraintFixture.checkConstraintWithExpr("column1 > 0");
      var command = ConstraintFixture.changeCheckExprCommand(constraint.id(), "   ");
      var columns = List.of(ConstraintFixture.constraintColumn("cc1", constraint.id(), "col1", 0));

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(constraint.tableId()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraint.id()))
          .willReturn(Mono.just(columns));
      given(changeConstraintExpressionPort.changeConstraintExpressions(
          constraint.id(), null, null))
              .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintCheckExpr(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeConstraintExpressionPort).should()
          .changeConstraintExpressions(eq(constraint.id()), eq((String) null), eq((String) null));
    }

  }

  @Nested
  @DisplayName("changeConstraintDefaultExpr 메서드는")
  class ChangeConstraintDefaultExpr {

    @Test
    @DisplayName("DEFAULT 제약조건의 표현식을 변경한다")
    void changesDefaultExprWhenKindIsDefault() {
      var constraint = ConstraintFixture.defaultConstraintWithExpr("0");
      var command = ConstraintFixture.changeDefaultExprCommand(constraint.id(), "  1  ");
      var columns = List.of(ConstraintFixture.constraintColumn("cc1", constraint.id(), "col1", 0));

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(constraint.tableId()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraint.id()))
          .willReturn(Mono.just(columns));
      given(changeConstraintExpressionPort.changeConstraintExpressions(
          constraint.id(), null, "1"))
              .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintDefaultExpr(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeConstraintExpressionPort).should()
          .changeConstraintExpressions(eq(constraint.id()), eq((String) null), eq("1"));
    }

    @Test
    @DisplayName("DEFAULT 이외의 kind에서는 예외가 발생한다")
    void throwsWhenKindMismatch() {
      var constraint = ConstraintFixture.checkConstraint();
      var command = ConstraintFixture.changeDefaultExprCommand(constraint.id(), "0");

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));

      StepVerifier.create(sut.changeConstraintDefaultExpr(command))
          .expectError(InvalidValueException.class)
          .verify();

      then(changeConstraintExpressionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("표현식이 null이면 DEFAULT 표현식을 제거한다")
    void clearsDefaultExprWhenNull() {
      var constraint = ConstraintFixture.defaultConstraintWithExpr("0");
      var command = ConstraintFixture.changeDefaultExprCommand(constraint.id(), null);
      var columns = List.of(ConstraintFixture.constraintColumn("cc1", constraint.id(), "col1", 0));

      given(getConstraintByIdPort.findConstraintById(constraint.id()))
          .willReturn(Mono.just(constraint));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(constraint.tableId()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraint.id()))
          .willReturn(Mono.just(columns));
      given(changeConstraintExpressionPort.changeConstraintExpressions(
          constraint.id(), null, null))
              .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintDefaultExpr(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeConstraintExpressionPort).should()
          .changeConstraintExpressions(eq(constraint.id()), eq((String) null), eq((String) null));
    }

  }

}
