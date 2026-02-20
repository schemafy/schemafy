package com.schemafy.domain.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetRelationshipService")
class GetRelationshipServiceTest {

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @InjectMocks
  GetRelationshipService sut;

  @Nested
  @DisplayName("getRelationship 메서드는")
  class GetRelationship {

    @Test
    @DisplayName("존재하는 관계를 반환한다")
    void returnsExistingRelationship() {
      var query = RelationshipFixture.getRelationshipQuery();
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));

      StepVerifier.create(sut.getRelationship(query))
          .assertNext(result -> {
            assertThat(result.id()).isEqualTo(RelationshipFixture.DEFAULT_ID);
            assertThat(result.name()).isEqualTo(RelationshipFixture.DEFAULT_NAME);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 예외가 발생한다")
    void throwsWhenNotExists() {
      var query = RelationshipFixture.getRelationshipQuery();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.getRelationship(query))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.NOT_FOUND))
          .verify();
    }

  }

}
