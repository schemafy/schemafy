package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetConstraintsByTableIdService")
class GetConstraintsByTableIdServiceTest {

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @InjectMocks
  GetConstraintsByTableIdService sut;

  @Nested
  @DisplayName("getConstraintsByTableId 메서드는")
  class GetConstraintsByTableId {

    @Test
    @DisplayName("테이블의 모든 제약조건을 반환한다")
    void returnsConstraintsForTable() {
      var query = ConstraintFixture.getConstraintsByTableIdQuery();
      var constraints = List.of(
          ConstraintFixture.primaryKeyConstraintWithId("pk1"),
          ConstraintFixture.uniqueConstraintWithId("uq1"));

      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(constraints));

      StepVerifier.create(sut.getConstraintsByTableId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("테이블에 제약조건이 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoConstraints() {
      var query = ConstraintFixture.getConstraintsByTableIdQuery();

      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getConstraintsByTableId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }

  }

}
