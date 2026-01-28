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
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeConstraintColumnPositionService")
class ChangeConstraintColumnPositionServiceTest {

  @Mock
  ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;

  @Mock
  GetConstraintColumnByIdPort getConstraintColumnByIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @InjectMocks
  ChangeConstraintColumnPositionService sut;

  @Nested
  @DisplayName("changeConstraintColumnPosition 메서드는")
  class ChangeConstraintColumnPosition {

    @Test
    @DisplayName("유효한 위치로 컬럼 순서를 변경한다")
    void changesPositionWithValidSeqNo() {
      var command = ConstraintFixture.changeColumnPositionCommand("cc1", 1);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0);
      var columns = List.of(
          constraintColumn,
          ConstraintFixture.constraintColumn("cc2", "constraint1", "col2", 1),
          ConstraintFixture.constraintColumn("cc3", "constraint1", "col3", 2));

      given(getConstraintColumnByIdPort.findConstraintColumnById(any()))
          .willReturn(Mono.just(constraintColumn));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(columns));
      given(changeConstraintColumnPositionPort.changeConstraintColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintColumnPosition(command))
          .verifyComplete();

      then(changeConstraintColumnPositionPort).should()
          .changeConstraintColumnPositions(eq("constraint1"), anyList());
    }

    @Test
    @DisplayName("음수 위치면 예외가 발생한다")
    void throwsWhenNegativePosition() {
      var command = ConstraintFixture.changeColumnPositionCommand("cc1", -1);

      StepVerifier.create(sut.changeConstraintColumnPosition(command))
          .expectError(ConstraintPositionInvalidException.class)
          .verify();

      then(changeConstraintColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("범위를 벗어난 위치면 예외가 발생한다")
    void throwsWhenPositionOutOfRange() {
      var command = ConstraintFixture.changeColumnPositionCommand("cc1", 5);
      var constraintColumn = ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0);
      var columns = List.of(
          constraintColumn,
          ConstraintFixture.constraintColumn("cc2", "constraint1", "col2", 1));

      given(getConstraintColumnByIdPort.findConstraintColumnById(any()))
          .willReturn(Mono.just(constraintColumn));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(columns));

      StepVerifier.create(sut.changeConstraintColumnPosition(command))
          .expectError(ConstraintPositionInvalidException.class)
          .verify();

      then(changeConstraintColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintColumnNotExists() {
      var command = ConstraintFixture.changeColumnPositionCommand("nonexistent", 0);

      given(getConstraintColumnByIdPort.findConstraintColumnById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintColumnPosition(command))
          .expectError(RuntimeException.class)
          .verify();

      then(changeConstraintColumnPositionPort).shouldHaveNoInteractions();
    }

  }

}
