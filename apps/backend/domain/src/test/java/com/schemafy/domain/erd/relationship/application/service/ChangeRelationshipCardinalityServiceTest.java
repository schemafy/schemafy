package com.schemafy.domain.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipCardinalityPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeRelationshipCardinalityService")
class ChangeRelationshipCardinalityServiceTest {

  @Mock
  ChangeRelationshipCardinalityPort changeRelationshipCardinalityPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @InjectMocks
  ChangeRelationshipCardinalityService sut;

  @Nested
  @DisplayName("changeRelationshipCardinality 메서드는")
  class ChangeRelationshipCardinality {

    @Test
    @DisplayName("ONE_TO_ONE에서 ONE_TO_MANY로 변경한다")
    void changesFromOneToOneToOneToMany() {
      var command = RelationshipFixture.changeCardinalityCommand(Cardinality.ONE_TO_MANY);
      var relationship = RelationshipFixture.relationshipWithCardinality(Cardinality.ONE_TO_ONE);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(changeRelationshipCardinalityPort.changeRelationshipCardinality(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipCardinality(command))
          .verifyComplete();

      then(changeRelationshipCardinalityPort).should()
          .changeRelationshipCardinality(eq(relationship.id()), eq(Cardinality.ONE_TO_MANY));
    }

    @Test
    @DisplayName("ONE_TO_MANY에서 ONE_TO_ONE으로 변경한다")
    void changesFromOneToManyToOneToOne() {
      var command = RelationshipFixture.changeCardinalityCommand(Cardinality.ONE_TO_ONE);
      var relationship = RelationshipFixture.relationshipWithCardinality(Cardinality.ONE_TO_MANY);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(changeRelationshipCardinalityPort.changeRelationshipCardinality(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipCardinality(command))
          .verifyComplete();

      then(changeRelationshipCardinalityPort).should()
          .changeRelationshipCardinality(eq(relationship.id()), eq(Cardinality.ONE_TO_ONE));
    }

    @Test
    @DisplayName("관계가 존재하지 않으면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      var command = RelationshipFixture.changeCardinalityCommand(Cardinality.ONE_TO_ONE);

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipCardinality(command))
          .expectError(RelationshipNotExistException.class)
          .verify();

      then(changeRelationshipCardinalityPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("cardinality가 null이면 예외가 발생한다")
    void throwsWhenCardinalityIsNull() {
      var command = new ChangeRelationshipCardinalityCommand(
          RelationshipFixture.DEFAULT_ID, null);

      StepVerifier.create(sut.changeRelationshipCardinality(command))
          .expectError(IllegalArgumentException.class)
          .verify();

      then(changeRelationshipCardinalityPort).shouldHaveNoInteractions();
    }

  }

}
