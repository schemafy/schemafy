package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveRelationshipColumnService")
class RemoveRelationshipColumnServiceTest {

  @Mock
  DeleteRelationshipColumnPort deleteRelationshipColumnPort;

  @Mock
  DeleteRelationshipPort deleteRelationshipPort;

  @Mock
  ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  DeleteColumnUseCase deleteColumnUseCase;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  RemoveRelationshipColumnService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("removeRelationshipColumn 메서드는")
  class RemoveRelationshipColumn {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("관계 컬럼을 삭제한다")
      void removesRelationshipColumn() {
        var command = RelationshipFixture.removeColumnCommand();
        var relationship = RelationshipFixture.defaultRelationship();
        var columnToRemove = RelationshipFixture.defaultRelationshipColumn();
        var remainingColumn = RelationshipFixture.relationshipColumn(
            "remaining", RelationshipFixture.DEFAULT_ID, "pk2", "fk2", 1);

        given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
            .willReturn(Mono.just(columnToRemove));
        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(deleteRelationshipColumnPort.deleteRelationshipColumn(any()))
            .willReturn(Mono.empty());
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(any()))
            .willReturn(Mono.just(List.of(remainingColumn)));
        given(changeRelationshipColumnPositionPort
            .changeRelationshipColumnPositions(any(), anyList()))
            .willReturn(Mono.empty());
        given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.removeRelationshipColumn(command))
            .expectNextCount(1)
            .verifyComplete();

        then(deleteRelationshipColumnPort).should()
            .deleteRelationshipColumn(columnToRemove.id());
        then(deleteRelationshipPort).should(never()).deleteRelationship(any());
      }

      @Test
      @DisplayName("남은 컬럼의 seqNo를 재정렬한다")
      void reordersRemainingColumnsSeqNo() {
        var command = RelationshipFixture.removeColumnCommand();
        var relationship = RelationshipFixture.defaultRelationship();
        var columnToRemove = RelationshipFixture.defaultRelationshipColumn();
        var remaining1 = RelationshipFixture.relationshipColumn(
            "col1", RelationshipFixture.DEFAULT_ID, "pk1", "fk1", 2);
        var remaining2 = RelationshipFixture.relationshipColumn(
            "col2", RelationshipFixture.DEFAULT_ID, "pk2", "fk2", 3);

        given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
            .willReturn(Mono.just(columnToRemove));
        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(deleteRelationshipColumnPort.deleteRelationshipColumn(any()))
            .willReturn(Mono.empty());
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(any()))
            .willReturn(Mono.just(List.of(remaining1, remaining2)));
        given(changeRelationshipColumnPositionPort
            .changeRelationshipColumnPositions(any(), anyList()))
            .willReturn(Mono.empty());
        given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.removeRelationshipColumn(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeRelationshipColumnPositionPort).should()
            .changeRelationshipColumnPositions(eq(relationship.id()), anyList());
      }

    }

    @Nested
    @DisplayName("마지막 컬럼을 삭제하면")
    class WhenRemovingLastColumn {

      @Test
      @DisplayName("관계 자체를 삭제한다")
      void deletesRelationshipWhenLastColumn() {
        var command = RelationshipFixture.removeColumnCommand();
        var relationship = RelationshipFixture.defaultRelationship();
        var columnToRemove = RelationshipFixture.defaultRelationshipColumn();

        given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
            .willReturn(Mono.just(columnToRemove));
        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(deleteRelationshipColumnPort.deleteRelationshipColumn(any()))
            .willReturn(Mono.empty());
        given(getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(any()))
            .willReturn(Mono.just(List.of()));
        given(deleteRelationshipPort.deleteRelationship(any()))
            .willReturn(Mono.empty());
        given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.removeRelationshipColumn(command))
            .expectNextCount(1)
            .verifyComplete();

        then(deleteRelationshipColumnPort).should()
            .deleteRelationshipColumn(columnToRemove.id());
        then(deleteRelationshipPort).should()
            .deleteRelationship(relationship.id());
        then(changeRelationshipColumnPositionPort).should(never())
            .changeRelationshipColumnPositions(any(), anyList());
      }

    }

    @Nested
    @DisplayName("관계가 존재하지 않으면")
    class WhenRelationshipNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.removeColumnCommand();
        var relationshipColumn = RelationshipFixture.relationshipColumn(
            RelationshipFixture.DEFAULT_COLUMN_ID,
            "missing-relationship",
            RelationshipFixture.DEFAULT_PK_COLUMN_ID,
            RelationshipFixture.DEFAULT_FK_COLUMN_ID,
            0);

        given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
            .willReturn(Mono.just(relationshipColumn));
        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.removeRelationshipColumn(command))
            .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.NOT_FOUND))
            .verify();

        then(deleteRelationshipColumnPort).shouldHaveNoInteractions();
        then(deleteRelationshipPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("관계 컬럼이 존재하지 않으면")
    class WhenRelationshipColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.removeColumnCommand();
        given(getRelationshipColumnByIdPort.findRelationshipColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.removeRelationshipColumn(command))
            .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.COLUMN_NOT_FOUND))
            .verify();

        then(deleteRelationshipColumnPort).shouldHaveNoInteractions();
        then(deleteRelationshipPort).shouldHaveNoInteractions();
      }

    }

  }

}
