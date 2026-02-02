package com.schemafy.domain.erd.constraint.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetConstraintColumnService")
class GetConstraintColumnServiceTest {

  @Mock
  GetConstraintColumnByIdPort getConstraintColumnByIdPort;

  @InjectMocks
  GetConstraintColumnService sut;

  @Nested
  @DisplayName("getConstraintColumn 메서드는")
  class GetConstraintColumn {

    @Test
    @DisplayName("존재하는 제약조건 컬럼을 반환한다")
    void returnsExistingConstraintColumn() {
      var query = ConstraintFixture.getConstraintColumnQuery();
      var constraintColumn = ConstraintFixture.defaultConstraintColumn();

      given(getConstraintColumnByIdPort.findConstraintColumnById(any()))
          .willReturn(Mono.just(constraintColumn));

      StepVerifier.create(sut.getConstraintColumn(query))
          .assertNext(result -> {
            assertThat(result.id()).isEqualTo(ConstraintFixture.DEFAULT_CONSTRAINT_COLUMN_ID);
            assertThat(result.constraintId()).isEqualTo(ConstraintFixture.DEFAULT_ID);
            assertThat(result.columnId()).isEqualTo(ConstraintFixture.DEFAULT_COLUMN_ID);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 예외가 발생한다")
    void throwsWhenNotExists() {
      var query = ConstraintFixture.getConstraintColumnQuery();

      given(getConstraintColumnByIdPort.findConstraintColumnById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getConstraintColumn(query))
          .expectError(ConstraintColumnNotExistException.class)
          .verify();
    }

  }

}
