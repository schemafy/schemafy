package com.schemafy.domain.erd.table.application.service;

import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTableService")
class DeleteTableServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteTablePort deleteTablePort;

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

        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(deleteTablePort.deleteTable(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteTable(command))
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

    }

  }

}
