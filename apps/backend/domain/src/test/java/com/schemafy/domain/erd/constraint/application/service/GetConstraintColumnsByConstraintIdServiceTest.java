package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetConstraintColumnsByConstraintIdService")
class GetConstraintColumnsByConstraintIdServiceTest {

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @InjectMocks
  GetConstraintColumnsByConstraintIdService sut;

  @Nested
  @DisplayName("getConstraintColumnsByConstraintId 메서드는")
  class GetConstraintColumnsByConstraintId {

    @Test
    @DisplayName("제약조건의 모든 컬럼을 반환한다")
    void returnsColumnsForConstraint() {
      var query = ConstraintFixture.getConstraintColumnsByConstraintIdQuery();
      var columns = List.of(
          ConstraintFixture.constraintColumn("cc1", ConstraintFixture.DEFAULT_ID, "col1", 0),
          ConstraintFixture.constraintColumn("cc2", ConstraintFixture.DEFAULT_ID, "col2", 1));

      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(columns));

      StepVerifier.create(sut.getConstraintColumnsByConstraintId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(2);
            assertThat(result.get(0).seqNo()).isEqualTo(0);
            assertThat(result.get(1).seqNo()).isEqualTo(1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("제약조건에 컬럼이 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoColumns() {
      var query = ConstraintFixture.getConstraintColumnsByConstraintIdQuery();

      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.getConstraintColumnsByConstraintId(query))
          .assertNext(result -> assertThat(result).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("컬럼들이 seqNo 순으로 정렬되어 반환된다")
    void returnsColumnsSortedBySeqNo() {
      var query = ConstraintFixture.getConstraintColumnsByConstraintIdQuery();
      var columns = List.of(
          ConstraintFixture.constraintColumn("cc1", ConstraintFixture.DEFAULT_ID, "col1", 0),
          ConstraintFixture.constraintColumn("cc2", ConstraintFixture.DEFAULT_ID, "col2", 1),
          ConstraintFixture.constraintColumn("cc3", ConstraintFixture.DEFAULT_ID, "col3", 2));

      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(columns));

      StepVerifier.create(sut.getConstraintColumnsByConstraintId(query))
          .assertNext(result -> {
            assertThat(result).hasSize(3);
            assertThat(result.get(0).columnId()).isEqualTo("col1");
            assertThat(result.get(1).columnId()).isEqualTo("col2");
            assertThat(result.get(2).columnId()).isEqualTo("col3");
          })
          .verifyComplete();
    }

  }

}
