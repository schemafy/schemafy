package com.schemafy.domain.erd.relationship.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteRelationshipService")
class DeleteRelationshipServiceTest {

  @Mock
  DeleteRelationshipPort deleteRelationshipPort;

  @Mock
  DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort;

  @InjectMocks
  DeleteRelationshipService sut;

  @Nested
  @DisplayName("deleteRelationship 메서드는")
  class DeleteRelationship {

    @Test
    @DisplayName("관계 컬럼을 먼저 삭제 후 관계를 삭제한다")
    void deletesColumnsFirstThenRelationship() {
      var command = RelationshipFixture.deleteCommand();

      given(deleteRelationshipColumnsPort.deleteByRelationshipId(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteRelationship(command))
          .verifyComplete();

      then(deleteRelationshipColumnsPort).should()
          .deleteByRelationshipId(RelationshipFixture.DEFAULT_ID);
      then(deleteRelationshipPort).should()
          .deleteRelationship(RelationshipFixture.DEFAULT_ID);
    }

    @Test
    @DisplayName("관계 컬럼 삭제가 완료된 후 관계를 삭제한다")
    void deletesRelationshipAfterColumnsDeleted() {
      var command = RelationshipFixture.deleteCommand();

      given(deleteRelationshipColumnsPort.deleteByRelationshipId(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteRelationship(command))
          .verifyComplete();

      var inOrder = org.mockito.Mockito.inOrder(
          deleteRelationshipColumnsPort,
          deleteRelationshipPort);
      inOrder.verify(deleteRelationshipColumnsPort)
          .deleteByRelationshipId(RelationshipFixture.DEFAULT_ID);
      inOrder.verify(deleteRelationshipPort)
          .deleteRelationship(RelationshipFixture.DEFAULT_ID);
    }

  }

}
