package com.schemafy.core.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipExtraInverse;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeRelationshipExtraService")
class ChangeRelationshipExtraServiceTest {

  @Mock
  ChangeRelationshipExtraPort changeRelationshipExtraPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Spy
  JsonObjectMetadataConverter jsonObjectMetadataConverter = new JsonObjectMetadataConverter(
      new JsonCodec(new ObjectMapper().findAndRegisterModules()));

  @InjectMocks
  ChangeRelationshipExtraService sut;

  @Nested
  @DisplayName("changeRelationshipExtra 메서드는")
  class ChangeRelationshipExtra {

    @Test
    @DisplayName("extra 값을 설정한다")
    void setsExtraValue() {
      var command = RelationshipFixture.changeExtraCommand(
          RelationshipFixture.jsonObject("{\"description\": \"some extra info\"}"));
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));
      given(changeRelationshipExtraPort.changeRelationshipExtra(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .assertNext(result -> assertThat(result.inversePayload()).isEqualTo(
              new ChangeRelationshipExtraInverse(relationship.id(), relationship.extra())))
          .verifyComplete();

      then(changeRelationshipExtraPort).should()
          .changeRelationshipExtra(eq(relationship.id()),
              eq("{\"description\":\"some extra info\"}"));
    }

    @Test
    @DisplayName("extra 값이 이미 null이면 변경 없이 성공한다")
    void allowsNullExtra() {
      var command = RelationshipFixture.changeExtraCommand(null);
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectNextMatches(result -> result.operation() == null)
          .verifyComplete();

      then(changeRelationshipExtraPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("JSON null이 현재 null로 정규화되면 변경 없이 성공한다")
    void normalizesJsonNullToNull() {
      var command = RelationshipFixture.changeExtraCommand(NullNode.getInstance());
      var relationship = RelationshipFixture.defaultRelationship();

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(relationship));

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectNextMatches(result -> result.operation() == null)
          .verifyComplete();

      then(changeRelationshipExtraPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("락 획득 후 extra가 이미 요청값이면 no-op으로 성공한다")
    void returnsNoOpWhenLockedRelationshipAlreadyHasRequestedExtra() {
      var command = RelationshipFixture.changeExtraCommand(
          RelationshipFixture.jsonObject("{\"description\": \"some extra info\"}"));
      var initialRelationship = RelationshipFixture.defaultRelationship();
      var lockedRelationship = RelationshipFixture.relationshipWithExtra(
          "{\"description\":\"some extra info\"}");

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.just(initialRelationship), Mono.just(lockedRelationship));

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectNextMatches(result -> result.noOp() && result.operation() == null)
          .verifyComplete();

      then(changeRelationshipExtraPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관계가 존재하지 않으면 예외가 발생한다")
    void throwsWhenRelationshipNotExists() {
      var command = RelationshipFixture.changeExtraCommand(
          RelationshipFixture.jsonObject("{\"description\": \"some info\"}"));

      given(getRelationshipByIdPort.findRelationshipById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.NOT_FOUND))
          .verify();

      then(changeRelationshipExtraPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("JSON object가 아니면 error signal로 반환한다")
    void returnsErrorSignalWhenExtraIsNotObject() {
      var command = RelationshipFixture.changeExtraCommand(TextNode.valueOf("invalid"));

      StepVerifier.create(sut.changeRelationshipExtra(command))
          .expectError(IllegalArgumentException.class)
          .verify();

      then(getRelationshipByIdPort).shouldHaveNoInteractions();
      then(changeRelationshipExtraPort).shouldHaveNoInteractions();
    }

  }

}
