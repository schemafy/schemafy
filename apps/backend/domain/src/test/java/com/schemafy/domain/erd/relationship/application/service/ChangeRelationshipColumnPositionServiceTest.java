package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipPositionInvalidException;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeRelationshipColumnPositionService")
class ChangeRelationshipColumnPositionServiceTest {

  @Mock
  ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @InjectMocks
  ChangeRelationshipColumnPositionService sut;

  @Nested
  @DisplayName("changeRelationshipColumnPosition 메서드는")
  class ChangeRelationshipColumnPosition {

    @Test
    @DisplayName("컬럼 위치를 변경한다")
    void changesColumnPosition() {
      var command = new ChangeRelationshipColumnPositionCommand(
          RelationshipFixture.DEFAULT_COLUMN_ID, 1);
      var column1 = RelationshipFixture.relationshipColumn(
          RelationshipFixture.DEFAULT_COLUMN_ID, RelationshipFixture.DEFAULT_ID, "pk1", "fk1", 0);
      var column2 = RelationshipFixture.relationshipColumn(
          "col2", RelationshipFixture.DEFAULT_ID, "pk2", "fk2", 1);

      given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
          .willReturn(Mono.just(column1));
      given(getRelationshipByIdPort.findRelationshipById(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(RelationshipFixture.defaultRelationship()));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(column1, column2)));
      given(changeRelationshipColumnPositionPort.changeRelationshipColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipColumnPosition(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeRelationshipColumnPositionPort).should()
          .changeRelationshipColumnPositions(eq(RelationshipFixture.DEFAULT_ID), anyList());
    }

    @Test
    @DisplayName("첫 번째 위치로 이동할 수 있다")
    void canMoveToFirstPosition() {
      var command = new ChangeRelationshipColumnPositionCommand("col2", 0);
      var column1 = RelationshipFixture.relationshipColumn(
          "col1", RelationshipFixture.DEFAULT_ID, "pk1", "fk1", 0);
      var column2 = RelationshipFixture.relationshipColumn(
          "col2", RelationshipFixture.DEFAULT_ID, "pk2", "fk2", 1);

      given(getRelationshipColumnByIdPort.findRelationshipColumnById("col2"))
          .willReturn(Mono.just(column2));
      given(getRelationshipByIdPort.findRelationshipById(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(RelationshipFixture.defaultRelationship()));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(column1, column2)));
      given(changeRelationshipColumnPositionPort.changeRelationshipColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipColumnPosition(command))
          .expectNextCount(1)
          .verifyComplete();
    }

    @Test
    @DisplayName("컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExists() {
      var command = new ChangeRelationshipColumnPositionCommand("non_existent", 0);

      given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeRelationshipColumnPosition(command))
          .expectError(RelationshipPositionInvalidException.class)
          .verify();

      then(changeRelationshipColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("음수 위치면 예외가 발생한다")
    void throwsWhenNegativePosition() {
      var command = new ChangeRelationshipColumnPositionCommand(
          RelationshipFixture.DEFAULT_COLUMN_ID, -1);

      StepVerifier.create(sut.changeRelationshipColumnPosition(command))
          .expectError(RelationshipPositionInvalidException.class)
          .verify();

      then(changeRelationshipColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("범위를 초과하는 위치면 예외가 발생한다")
    void throwsWhenPositionOutOfRange() {
      var command = new ChangeRelationshipColumnPositionCommand(
          RelationshipFixture.DEFAULT_COLUMN_ID, 5);
      var column = RelationshipFixture.defaultRelationshipColumn();

      given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
          .willReturn(Mono.just(column));
      given(getRelationshipByIdPort.findRelationshipById(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(RelationshipFixture.defaultRelationship()));
      given(getRelationshipColumnsByRelationshipIdPort.findRelationshipColumnsByRelationshipId(any()))
          .willReturn(Mono.just(List.of(column)));

      StepVerifier.create(sut.changeRelationshipColumnPosition(command))
          .expectError(RelationshipPositionInvalidException.class)
          .verify();

      then(changeRelationshipColumnPositionPort).shouldHaveNoInteractions();
    }

  }

}
