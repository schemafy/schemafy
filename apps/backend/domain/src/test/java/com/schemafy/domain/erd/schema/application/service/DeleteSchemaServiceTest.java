package com.schemafy.domain.erd.schema.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.CascadeDeleteConstraintsByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.CascadeDeleteIndexesByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.CascadeDeleteRelationshipsByTableIdPort;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.schema.fixture.SchemaFixture;
import com.schemafy.domain.erd.table.application.port.out.CascadeDeleteTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSchemaService")
class DeleteSchemaServiceTest {

  private static final String TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5TAB";

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteSchemaPort deleteSchemaPort;

  @Mock
  GetTablesBySchemaIdPort getTablesBySchemaIdPort;

  @Mock
  CascadeDeleteTablesBySchemaIdPort cascadeDeleteTablesPort;

  @Mock
  DeleteColumnsByTableIdPort deleteColumnsByTableIdPort;

  @Mock
  CascadeDeleteConstraintsByTableIdPort cascadeDeleteConstraintsPort;

  @Mock
  CascadeDeleteIndexesByTableIdPort cascadeDeleteIndexesPort;

  @Mock
  CascadeDeleteRelationshipsByTableIdPort cascadeDeleteRelationshipsPort;

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

        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.just(table));
        given(cascadeDeleteConstraintsPort.cascadeDeleteByTableId(any()))
            .willReturn(Mono.empty());
        given(cascadeDeleteIndexesPort.cascadeDeleteByTableId(any()))
            .willReturn(Mono.empty());
        given(cascadeDeleteRelationshipsPort.cascadeDeleteByTableId(any()))
            .willReturn(Mono.empty());
        given(deleteColumnsByTableIdPort.deleteColumnsByTableId(any()))
            .willReturn(Mono.empty());
        given(cascadeDeleteTablesPort.cascadeDeleteBySchemaId(any()))
            .willReturn(Mono.empty());
        given(deleteSchemaPort.deleteSchema(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteSchema(command))
            .verifyComplete();

        then(getTablesBySchemaIdPort).should()
            .findTablesBySchemaId(command.schemaId());
        then(cascadeDeleteConstraintsPort).should()
            .cascadeDeleteByTableId(TABLE_ID);
        then(cascadeDeleteIndexesPort).should()
            .cascadeDeleteByTableId(TABLE_ID);
        then(cascadeDeleteRelationshipsPort).should()
            .cascadeDeleteByTableId(TABLE_ID);
        then(deleteColumnsByTableIdPort).should()
            .deleteColumnsByTableId(TABLE_ID);
        then(cascadeDeleteTablesPort).should()
            .cascadeDeleteBySchemaId(command.schemaId());
        then(deleteSchemaPort).should()
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

        given(getTablesBySchemaIdPort.findTablesBySchemaId(any()))
            .willReturn(Flux.empty());
        given(cascadeDeleteTablesPort.cascadeDeleteBySchemaId(any()))
            .willReturn(Mono.empty());
        given(deleteSchemaPort.deleteSchema(any()))
            .willReturn(Mono.empty());
        given(transactionalOperator.transactional(any(Mono.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(sut.deleteSchema(command))
            .verifyComplete();

        then(cascadeDeleteConstraintsPort).shouldHaveNoInteractions();
        then(cascadeDeleteIndexesPort).shouldHaveNoInteractions();
        then(cascadeDeleteRelationshipsPort).shouldHaveNoInteractions();
        then(deleteColumnsByTableIdPort).shouldHaveNoInteractions();
        then(deleteSchemaPort).should()
            .deleteSchema(command.schemaId());
      }
    }
  }

}
