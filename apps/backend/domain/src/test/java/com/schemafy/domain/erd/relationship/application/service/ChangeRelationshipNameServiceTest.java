package com.schemafy.domain.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameInvalidException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeRelationshipNameService")
class ChangeRelationshipNameServiceTest {

  @Mock
  ChangeRelationshipNamePort changeRelationshipNamePort;

  @Mock
  RelationshipExistsPort relationshipExistsPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @InjectMocks
  ChangeRelationshipNameService sut;

  @Nested
  @DisplayName("changeRelationshipName 메서드는")
  class ChangeRelationshipName {

    @Test
    @DisplayName("관계 이름을 변경한다")
    void changesRelationshipName() {
      var command = RelationshipFixture.changeNameCommand("new_name");
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(relationshipExistsPort.existsByFkTableIdAndNameExcludingId(any(), any(), any()))
          .willReturn(Mono.just(false));
      given(changeRelationshipNamePort.changeRelationshipName(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipName(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeRelationshipNamePort).should()
          .changeRelationshipName(eq(relationship.id()), eq("new_name"));
    }

    @Test
    @DisplayName("관계가 존재하지 않으면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      var command = RelationshipFixture.changeNameCommand("new_name");

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipName(command))
          .expectError(RelationshipNotExistException.class)
          .verify();

      then(changeRelationshipNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 유효하지 않으면 예외가 발생한다")
    void throwsWhenNameInvalid() {
      var command = RelationshipFixture.changeNameCommand("");

      StepVerifier.create(sut.changeRelationshipName(command))
          .expectError(RelationshipNameInvalidException.class)
          .verify();

      then(changeRelationshipNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 중복되면 예외가 발생한다")
    void throwsWhenNameDuplicate() {
      var command = RelationshipFixture.changeNameCommand("duplicate_name");
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(relationshipExistsPort.existsByFkTableIdAndNameExcludingId(any(), any(), any()))
          .willReturn(Mono.just(true));

      StepVerifier.create(sut.changeRelationshipName(command))
          .expectError(RelationshipNameDuplicateException.class)
          .verify();

      then(changeRelationshipNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 이름으로 변경 시도해도 자기 자신은 제외하고 검사한다")
    void excludesSelfWhenCheckingDuplicate() {
      var command = RelationshipFixture.changeNameCommand(RelationshipFixture.DEFAULT_NAME);
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(relationshipExistsPort.existsByFkTableIdAndNameExcludingId(
          eq(relationship.fkTableId()), eq(RelationshipFixture.DEFAULT_NAME), eq(relationship.id())))
          .willReturn(Mono.just(false));
      given(changeRelationshipNamePort.changeRelationshipName(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipName(command))
          .expectNextCount(1)
          .verifyComplete();
    }

  }

}
