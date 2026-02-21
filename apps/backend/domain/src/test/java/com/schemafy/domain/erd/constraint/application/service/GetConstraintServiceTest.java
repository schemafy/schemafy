package com.schemafy.domain.erd.constraint.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetConstraintService")
class GetConstraintServiceTest {

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @InjectMocks
  GetConstraintService sut;

  @Nested
  @DisplayName("getConstraint 메서드는")
  class GetConstraint {

    @Test
    @DisplayName("존재하는 제약조건을 반환한다")
    void returnsExistingConstraint() {
      var query = ConstraintFixture.getConstraintQuery();
      var constraint = ConstraintFixture.defaultConstraint();

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));

      StepVerifier.create(sut.getConstraint(query))
          .assertNext(result -> {
            assertThat(result.id()).isEqualTo(ConstraintFixture.DEFAULT_ID);
            assertThat(result.name()).isEqualTo(ConstraintFixture.DEFAULT_NAME);
            assertThat(result.kind()).isEqualTo(ConstraintFixture.DEFAULT_KIND);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 예외가 발생한다")
    void throwsWhenNotExists() {
      var query = ConstraintFixture.getConstraintQuery();

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getConstraint(query))
          .expectErrorMatches(DomainException.hasErrorCode(ConstraintErrorCode.NOT_FOUND))
          .verify();
    }

  }

}
