package com.schemafy.domain.erd.schema.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.CascadeDeleteConstraintsByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.CascadeDeleteIndexesByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.CascadeDeleteRelationshipsByTableIdPort;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.table.application.port.out.CascadeDeleteTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSchemaService 테스트")
class DeleteSchemaServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
  private static final String TABLE_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5FA1";
  private static final String TABLE_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5FA2";

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
  DeleteSchemaService deleteSchemaService;

  @Test
  @DisplayName("스키마 삭제 성공")
  void deleteSchema_Success() {
    DeleteSchemaCommand command = new DeleteSchemaCommand(SCHEMA_ID);

    given(getTablesBySchemaIdPort.findTablesBySchemaId(SCHEMA_ID))
        .willReturn(Flux.empty());
    given(cascadeDeleteTablesPort.cascadeDeleteBySchemaId(SCHEMA_ID))
        .willReturn(Mono.empty());
    given(deleteSchemaPort.deleteSchema(SCHEMA_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(deleteSchemaService.deleteSchema(command))
        .verifyComplete();

    verify(getTablesBySchemaIdPort).findTablesBySchemaId(SCHEMA_ID);
    verify(cascadeDeleteTablesPort).cascadeDeleteBySchemaId(SCHEMA_ID);
    verify(deleteSchemaPort).deleteSchema(SCHEMA_ID);
  }

  @Test
  @DisplayName("스키마 삭제 시 모든 관련 엔티티 cascade 삭제")
  void deleteSchema_CascadeDeletesAllRelatedEntities() {
    DeleteSchemaCommand command = new DeleteSchemaCommand(SCHEMA_ID);
    Table table1 = new Table(TABLE_ID_1, SCHEMA_ID, "table1", "utf8mb4", "utf8mb4_general_ci");
    Table table2 = new Table(TABLE_ID_2, SCHEMA_ID, "table2", "utf8mb4", "utf8mb4_general_ci");

    given(getTablesBySchemaIdPort.findTablesBySchemaId(SCHEMA_ID))
        .willReturn(Flux.just(table1, table2));
    given(cascadeDeleteConstraintsPort.cascadeDeleteByTableId(TABLE_ID_1))
        .willReturn(Mono.empty());
    given(cascadeDeleteConstraintsPort.cascadeDeleteByTableId(TABLE_ID_2))
        .willReturn(Mono.empty());
    given(cascadeDeleteIndexesPort.cascadeDeleteByTableId(TABLE_ID_1))
        .willReturn(Mono.empty());
    given(cascadeDeleteIndexesPort.cascadeDeleteByTableId(TABLE_ID_2))
        .willReturn(Mono.empty());
    given(cascadeDeleteRelationshipsPort.cascadeDeleteByTableId(TABLE_ID_1))
        .willReturn(Mono.empty());
    given(cascadeDeleteRelationshipsPort.cascadeDeleteByTableId(TABLE_ID_2))
        .willReturn(Mono.empty());
    given(deleteColumnsByTableIdPort.deleteColumnsByTableId(TABLE_ID_1))
        .willReturn(Mono.empty());
    given(deleteColumnsByTableIdPort.deleteColumnsByTableId(TABLE_ID_2))
        .willReturn(Mono.empty());
    given(cascadeDeleteTablesPort.cascadeDeleteBySchemaId(SCHEMA_ID))
        .willReturn(Mono.empty());
    given(deleteSchemaPort.deleteSchema(SCHEMA_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(deleteSchemaService.deleteSchema(command))
        .verifyComplete();

    verify(cascadeDeleteConstraintsPort).cascadeDeleteByTableId(TABLE_ID_1);
    verify(cascadeDeleteConstraintsPort).cascadeDeleteByTableId(TABLE_ID_2);
    verify(cascadeDeleteIndexesPort).cascadeDeleteByTableId(TABLE_ID_1);
    verify(cascadeDeleteIndexesPort).cascadeDeleteByTableId(TABLE_ID_2);
    verify(cascadeDeleteRelationshipsPort).cascadeDeleteByTableId(TABLE_ID_1);
    verify(cascadeDeleteRelationshipsPort).cascadeDeleteByTableId(TABLE_ID_2);
    verify(deleteColumnsByTableIdPort).deleteColumnsByTableId(TABLE_ID_1);
    verify(deleteColumnsByTableIdPort).deleteColumnsByTableId(TABLE_ID_2);
    verify(cascadeDeleteTablesPort).cascadeDeleteBySchemaId(SCHEMA_ID);
    verify(deleteSchemaPort).deleteSchema(SCHEMA_ID);
  }

}
