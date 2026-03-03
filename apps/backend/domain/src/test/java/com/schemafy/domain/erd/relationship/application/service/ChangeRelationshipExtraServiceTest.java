package com.schemafy.domain.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeRelationshipExtraService")
class ChangeRelationshipExtraServiceTest {

  @Mock
  ChangeRelationshipExtraPort changeRelationshipExtraPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @InjectMocks
  ChangeRelationshipExtraService sut;

  @Nested
  @DisplayName("changeRelationshipExtra 메서드는")
  class ChangeRelationshipExtra {

    @Test
    @DisplayName("extra 값을 설정한다")
    void setsExtraValue() {
      var command = RelationshipFixture.changeExtraCommand("some extra info");
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(changeRelationshipExtraPort.changeRelationshipExtra(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeRelationshipExtraPort).should()
          .changeRelationshipExtra(eq(relationship.id()), eq("some extra info"));
    }

    @Test
    @DisplayName("extra 값을 null로 설정할 수 있다")
    void allowsNullExtra() {
      var command = RelationshipFixture.changeExtraCommand(null);
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(changeRelationshipExtraPort.changeRelationshipExtra(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeRelationshipExtraPort).should()
          .changeRelationshipExtra(eq(relationship.id()), isNull());
    }

    @Test
    @DisplayName("빈 문자열은 null로 정규화된다")
    void normalizesBlankToNull() {
      var command = RelationshipFixture.changeExtraCommand("   ");
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(changeRelationshipExtraPort.changeRelationshipExtra(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeRelationshipExtraPort).should()
          .changeRelationshipExtra(eq(relationship.id()), isNull());
    }

    @Test
    @DisplayName("관계가 존재하지 않으면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      var command = RelationshipFixture.changeExtraCommand("some info");

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.NOT_FOUND))
          .verify();

      then(changeRelationshipExtraPort).shouldHaveNoInteractions();
    }

  }

}
