package com.schemafy.domain.erd.table.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTableService")
class DeleteTableServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteTablePort deleteTablePort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;

  @Mock
  DeleteRelationshipUseCase deleteRelationshipUseCase;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  DeleteColumnUseCase deleteColumnUseCase;

  @InjectMocks
  DeleteTableService sut;

  @Nested
  @DisplayName("deleteTable 메서드는")
  class DeleteTable {

    @Nested
    @DisplayName("삭제 요청이 주어지면")
    class WithDeleteRequest {

      @Test
      @DisplayName("관련 엔티티들을 순서대로 삭제한다")
      void deletesRelatedEntitiesInOrder() {
        var command = TableFixture.deleteCommand();
        var relationship = RelationshipFixture.defaultRelationship();
        var column = ColumnFixture.defaultColumn();

        given(getTableByIdPort.findTableById(command.tableId()))
            .willReturn(Mono.just(TableFixture.tableWithId(command.tableId())));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
            .willReturn(Mono.just(List.of(relationship)));
        given(deleteRelationshipUseCase.deleteRelationship(any(DeleteRelationshipCommand.class)))
            .willReturn(Mono.empty());
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
            .willReturn(Mono.empty());
        given(deleteTablePort.deleteTable(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .expectNextCount(1)
            .verifyComplete();

        var inOrderVerifier = inOrder(
            getRelationshipsByTableIdPort,
            deleteRelationshipUseCase,
            getColumnsByTableIdPort,
            deleteColumnUseCase,
            deleteTablePort);
        inOrderVerifier.verify(getRelationshipsByTableIdPort)
            .findRelationshipsByTableId(command.tableId());
        inOrderVerifier.verify(deleteRelationshipUseCase)
            .deleteRelationship(new DeleteRelationshipCommand(relationship.id()));
        inOrderVerifier.verify(getColumnsByTableIdPort)
            .findColumnsByTableId(command.tableId());
        inOrderVerifier.verify(deleteColumnUseCase)
            .deleteColumn(new DeleteColumnCommand(column.id()));
        inOrderVerifier.verify(deleteTablePort)
            .deleteTable(command.tableId());
      }

      @Test
      @DisplayName("관계/컬럼이 없으면 테이블만 삭제한다")
      void deletesOnlyTableWhenNoRelationsAndColumns() {
        var command = TableFixture.deleteCommand();

        given(getTableByIdPort.findTableById(command.tableId()))
            .willReturn(Mono.just(TableFixture.tableWithId(command.tableId())));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(deleteTablePort.deleteTable(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .expectNextCount(1)
            .verifyComplete();

        then(deleteRelationshipUseCase).shouldHaveNoInteractions();
        then(deleteColumnUseCase).shouldHaveNoInteractions();
        then(deleteTablePort).should().deleteTable(command.tableId());
      }

      @Test
      @DisplayName("여러 관계와 컬럼을 순서대로 삭제한다")
      void deletesMultipleRelationshipsAndColumnsInOrder() {
        var command = TableFixture.deleteCommand();
        var relationship1 = RelationshipFixture.relationshipWithId("rel-1");
        var relationship2 = RelationshipFixture.relationshipWithId("rel-2");
        var column1 = ColumnFixture.columnWithId("col-1");
        var column2 = ColumnFixture.columnWithId("col-2");

        given(getTableByIdPort.findTableById(command.tableId()))
            .willReturn(Mono.just(TableFixture.tableWithId(command.tableId())));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
            .willReturn(Mono.just(List.of(relationship1, relationship2)));
        given(deleteRelationshipUseCase.deleteRelationship(any(DeleteRelationshipCommand.class)))
            .willReturn(Mono.empty());
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column1, column2)));
        given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
            .willReturn(Mono.empty());
        given(deleteTablePort.deleteTable(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .expectNextCount(1)
            .verifyComplete();

        var inOrderVerifier = inOrder(
            getRelationshipsByTableIdPort,
            deleteRelationshipUseCase,
            getColumnsByTableIdPort,
            deleteColumnUseCase,
            deleteTablePort);
        inOrderVerifier.verify(getRelationshipsByTableIdPort)
            .findRelationshipsByTableId(command.tableId());
        inOrderVerifier.verify(deleteRelationshipUseCase)
            .deleteRelationship(eq(new DeleteRelationshipCommand(relationship1.id())));
        inOrderVerifier.verify(deleteRelationshipUseCase)
            .deleteRelationship(eq(new DeleteRelationshipCommand(relationship2.id())));
        inOrderVerifier.verify(getColumnsByTableIdPort)
            .findColumnsByTableId(command.tableId());
        inOrderVerifier.verify(deleteColumnUseCase)
            .deleteColumn(eq(new DeleteColumnCommand(column1.id())));
        inOrderVerifier.verify(deleteColumnUseCase)
            .deleteColumn(eq(new DeleteColumnCommand(column2.id())));
        inOrderVerifier.verify(deleteTablePort)
            .deleteTable(command.tableId());
      }

      @Test
      @DisplayName("초기 스냅샷의 관계가 cascade로 이미 삭제되었으면 건너뛰고 계속 삭제한다")
      void ignoresRelationshipsAlreadyDeletedByCascade() {
        var command = TableFixture.deleteCommand();
        var relationship1 = RelationshipFixture.relationshipWithId("rel-1");
        var relationship2 = RelationshipFixture.relationshipWithId("rel-2");
        var column = ColumnFixture.defaultColumn();

        given(getTableByIdPort.findTableById(command.tableId()))
            .willReturn(Mono.just(TableFixture.tableWithId(command.tableId())));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(command.tableId()))
            .willReturn(Mono.just(List.of(relationship1, relationship2)));
        given(deleteRelationshipUseCase.deleteRelationship(new DeleteRelationshipCommand(relationship1.id())))
            .willReturn(Mono.just(MutationResult.of(null, Set.of("cascade-table"))));
        given(deleteRelationshipUseCase.deleteRelationship(new DeleteRelationshipCommand(relationship2.id())))
            .willReturn(Mono.error(new DomainException(
                RelationshipErrorCode.NOT_FOUND,
                "Relationship not found: " + relationship2.id())));
        given(getColumnsByTableIdPort.findColumnsByTableId(command.tableId()))
            .willReturn(Mono.just(List.of(column)));
        given(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(column.id())))
            .willReturn(Mono.empty());
        given(deleteTablePort.deleteTable(command.tableId()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .assertNext(result -> {
              assertThat(result.affectedTableIds()).contains(command.tableId(), "cascade-table");
              assertThat(result.affectedTableIds()).doesNotContain("stale-relationship-table");
            })
            .verifyComplete();

        var inOrderVerifier = inOrder(
            getRelationshipsByTableIdPort,
            deleteRelationshipUseCase,
            getColumnsByTableIdPort,
            deleteColumnUseCase,
            deleteTablePort);
        inOrderVerifier.verify(getRelationshipsByTableIdPort)
            .findRelationshipsByTableId(command.tableId());
        inOrderVerifier.verify(deleteRelationshipUseCase)
            .deleteRelationship(eq(new DeleteRelationshipCommand(relationship1.id())));
        inOrderVerifier.verify(deleteRelationshipUseCase)
            .deleteRelationship(eq(new DeleteRelationshipCommand(relationship2.id())));
        inOrderVerifier.verify(getColumnsByTableIdPort)
            .findColumnsByTableId(command.tableId());
        inOrderVerifier.verify(deleteColumnUseCase)
            .deleteColumn(eq(new DeleteColumnCommand(column.id())));
        inOrderVerifier.verify(deleteTablePort)
            .deleteTable(command.tableId());
      }

      @Test
      @DisplayName("관계 삭제 중 NOT_FOUND 외 예외는 그대로 전파한다")
      void propagatesUnexpectedRelationshipDeletionErrors() {
        var command = TableFixture.deleteCommand();
        var relationship = RelationshipFixture.relationshipWithId("rel-1");

        given(getTableByIdPort.findTableById(command.tableId()))
            .willReturn(Mono.just(TableFixture.tableWithId(command.tableId())));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(command.tableId()))
            .willReturn(Mono.just(List.of(relationship)));
        given(deleteRelationshipUseCase.deleteRelationship(new DeleteRelationshipCommand(relationship.id())))
            .willReturn(Mono.error(new DomainException(
                RelationshipErrorCode.INVALID_VALUE,
                "Unexpected relationship deletion failure")));
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .expectErrorMatches(DomainException.hasErrorCode(RelationshipErrorCode.INVALID_VALUE))
            .verify();

        then(getColumnsByTableIdPort).shouldHaveNoInteractions();
        then(deleteColumnUseCase).shouldHaveNoInteractions();
        then(deleteTablePort).shouldHaveNoInteractions();
      }

      @Test
      @DisplayName("존재하지 않는 테이블이면 TableNotExistException이 발생한다")
      void rejectsDeletionWhenTableDoesNotExist() {
        var command = TableFixture.deleteCommand("missing-table");

        given(getTableByIdPort.findTableById(anyString()))
            .willReturn(Mono.empty());
        lenient().when(getRelationshipsByTableIdPort.findRelationshipsByTableId(anyString()))
            .thenReturn(Mono.just(List.of()));
        lenient().when(getColumnsByTableIdPort.findColumnsByTableId(anyString()))
            .thenReturn(Mono.just(List.of()));
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
            .expectErrorMatches(DomainException.hasErrorCode(TableErrorCode.NOT_FOUND))
            .verify();

        then(deleteTablePort).shouldHaveNoInteractions();
      }

    }

  }

}
