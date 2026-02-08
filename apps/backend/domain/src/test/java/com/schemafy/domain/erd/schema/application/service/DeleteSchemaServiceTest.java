package com.schemafy.domain.erd.schema.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.schema.fixture.SchemaFixture;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSchemaService")
class DeleteSchemaServiceTest {

  private static final String TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TAB";

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteSchemaPort deleteSchemaPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  GetTablesBySchemaIdPort getTablesBySchemaIdPort;

  @Mock
  DeleteTableUseCase deleteTableUseCase;

  @InjectMocks
  DeleteSchemaService sut;

  @Nested
  @DisplayName("deleteSchema 메서드는")
  class DeleteSchema {

    @Nested
    @DisplayName("스키마에 테이블이 있을 때")
    class WithTables {

      @Test
      @DisplayName("관련 엔티티들을 순서대로 삭제한다")
      void deletesRelatedEntitiesInOrder() {
        var command = SchemaFixture.deleteCommand();
        var table = new Table(TABLE_ID, SchemaFixture.DEFAULT_ID, "test_table", "utf8mb4",
            "utf8mb4_general_ci");

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.schemaWithId(command.schemaId())));
        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.just(table));
        given(deleteTableUseCase.deleteTable(any(DeleteTableCommand.class)))
            .willReturn(Mono.empty());
        given(deleteSchemaPort.deleteSchema(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteSchema(command))
            .expectNextCount(1)
            .verifyComplete();

        var inOrderVerifier = inOrder(getTablesBySchemaIdPort, deleteTableUseCase, deleteSchemaPort);
        inOrderVerifier.verify(getTablesBySchemaIdPort)
            .findTablesBySchemaId(command.schemaId());
        inOrderVerifier.verify(deleteTableUseCase)
            .deleteTable(new DeleteTableCommand(TABLE_ID));
        inOrderVerifier.verify(deleteSchemaPort)
            .deleteSchema(command.schemaId());
      }

      @Test
      @DisplayName("여러 테이블이 있으면 순서대로 삭제한다")
      void deletesMultipleTablesInOrder() {
        var command = SchemaFixture.deleteCommand();
        var table1 = new Table("table-1", SchemaFixture.DEFAULT_ID, "table_one", "utf8mb4",
            "utf8mb4_general_ci");
        var table2 = new Table("table-2", SchemaFixture.DEFAULT_ID, "table_two", "utf8mb4",
            "utf8mb4_general_ci");

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.schemaWithId(command.schemaId())));
        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.just(table1, table2));
        given(deleteTableUseCase.deleteTable(any(DeleteTableCommand.class)))
            .willReturn(Mono.empty());
        given(deleteSchemaPort.deleteSchema(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteSchema(command))
            .expectNextCount(1)
            .verifyComplete();

        var inOrderVerifier = inOrder(getTablesBySchemaIdPort, deleteTableUseCase, deleteSchemaPort);
        inOrderVerifier.verify(getTablesBySchemaIdPort)
            .findTablesBySchemaId(command.schemaId());
        inOrderVerifier.verify(deleteTableUseCase)
            .deleteTable(new DeleteTableCommand(table1.id()));
        inOrderVerifier.verify(deleteTableUseCase)
            .deleteTable(new DeleteTableCommand(table2.id()));
        inOrderVerifier.verify(deleteSchemaPort)
            .deleteSchema(command.schemaId());
      }

    }

    @Nested
    @DisplayName("스키마에 테이블이 없을 때")
    class WithoutTables {

      @Test
      @DisplayName("스키마만 삭제한다")
      void deletesOnlySchema() {
        var command = SchemaFixture.deleteCommand();

        given(getSchemaByIdPort.findSchemaById(command.schemaId()))
            .willReturn(Mono.just(SchemaFixture.schemaWithId(command.schemaId())));
        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.empty());
        given(deleteSchemaPort.deleteSchema(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteSchema(command))
            .expectNextCount(1)
            .verifyComplete();

        then(deleteTableUseCase).shouldHaveNoInteractions();
        then(deleteSchemaPort).should()
            .deleteSchema(command.schemaId());
      }

    }

    @Test
    @DisplayName("존재하지 않는 스키마이면 SchemaNotExistException이 발생한다")
    void rejectsDeletionWhenSchemaDoesNotExist() {
      var command = SchemaFixture.deleteCommand("missing-schema");

      given(getSchemaByIdPort.findSchemaById(anyString()))
          .willReturn(Mono.empty());
      given(getTablesBySchemaIdPort.findTablesBySchemaId(anyString()))
          .willReturn(Flux.empty());
      given(transactionalOperator.transactional(any(Mono.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      StepVerifier.create(sut.deleteSchema(command))
          .expectError(SchemaNotExistException.class)
          .verify();

      then(deleteSchemaPort).shouldHaveNoInteractions();
    }

  }

}
