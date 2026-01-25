package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.CascadeDeleteConstraintsByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.CascadeDeleteIndexesByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.CascadeDeleteRelationshipsByTableIdPort;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.table.application.port.out.CascadeDeleteTablesBySchemaIdPort;
import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteSchemaService implements DeleteSchemaUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteSchemaPort deleteSchemaPort;
  private final GetTablesBySchemaIdPort getTablesBySchemaIdPort;
  private final CascadeDeleteTablesBySchemaIdPort cascadeDeleteTablesPort;
  private final DeleteColumnsByTableIdPort deleteColumnsByTableIdPort;
  private final CascadeDeleteConstraintsByTableIdPort cascadeDeleteConstraintsPort;
  private final CascadeDeleteIndexesByTableIdPort cascadeDeleteIndexesPort;
  private final CascadeDeleteRelationshipsByTableIdPort cascadeDeleteRelationshipsPort;

  @Override
  public Mono<Void> deleteSchema(DeleteSchemaCommand command) {
    String schemaId = command.schemaId();
    return getTablesBySchemaIdPort.findTablesBySchemaId(schemaId)
        .flatMap(table -> Mono.when(
            cascadeDeleteConstraintsPort.cascadeDeleteByTableId(table.id()),
            cascadeDeleteIndexesPort.cascadeDeleteByTableId(table.id()),
            cascadeDeleteRelationshipsPort.cascadeDeleteByTableId(table.id())).then(deleteColumnsByTableIdPort
                .deleteColumnsByTableId(table.id())))
        .then(cascadeDeleteTablesPort.cascadeDeleteBySchemaId(schemaId))
        .then(deleteSchemaPort.deleteSchema(schemaId))
        .as(transactionalOperator::transactional);
  }

}
