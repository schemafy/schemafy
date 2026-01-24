package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.out.DeleteColumnsByTableIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.CascadeDeleteConstraintsByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.CascadeDeleteIndexesByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.CascadeDeleteRelationshipsByTableIdPort;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteTableService implements DeleteTableUseCase {

  private final DeleteTablePort deleteTablePort;
  private final DeleteColumnsByTableIdPort deleteColumnsByTableIdPort;
  private final CascadeDeleteConstraintsByTableIdPort cascadeDeleteConstraintsPort;
  private final CascadeDeleteIndexesByTableIdPort cascadeDeleteIndexesPort;
  private final CascadeDeleteRelationshipsByTableIdPort cascadeDeleteRelationshipsPort;

  @Override
  public Mono<Void> deleteTable(DeleteTableCommand command) {
    String tableId = command.tableId();
    return Mono.when(
        cascadeDeleteConstraintsPort.cascadeDeleteByTableId(tableId),
        cascadeDeleteIndexesPort.cascadeDeleteByTableId(tableId),
        cascadeDeleteRelationshipsPort.cascadeDeleteByTableId(tableId)).then(deleteColumnsByTableIdPort
            .deleteColumnsByTableId(tableId))
        .then(deleteTablePort.deleteTable(tableId));
  }

}
