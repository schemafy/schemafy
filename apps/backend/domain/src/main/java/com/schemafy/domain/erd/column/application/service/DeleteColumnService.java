package com.schemafy.domain.erd.column.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.DeleteColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByColumnIdPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteColumnService implements DeleteColumnUseCase {

  private final DeleteColumnPort deleteColumnPort;
  private final DeleteConstraintColumnsByColumnIdPort deleteConstraintColumnsPort;
  private final DeleteIndexColumnsByColumnIdPort deleteIndexColumnsPort;
  private final DeleteRelationshipColumnsByColumnIdPort deleteRelationshipColumnsPort;

  public DeleteColumnService(
      DeleteColumnPort deleteColumnPort,
      DeleteConstraintColumnsByColumnIdPort deleteConstraintColumnsPort,
      DeleteIndexColumnsByColumnIdPort deleteIndexColumnsPort,
      DeleteRelationshipColumnsByColumnIdPort deleteRelationshipColumnsPort) {
    this.deleteColumnPort = deleteColumnPort;
    this.deleteConstraintColumnsPort = deleteConstraintColumnsPort;
    this.deleteIndexColumnsPort = deleteIndexColumnsPort;
    this.deleteRelationshipColumnsPort = deleteRelationshipColumnsPort;
  }

  @Override
  public Mono<Void> deleteColumn(DeleteColumnCommand command) {
    String columnId = command.columnId();
    return Mono.when(
        deleteConstraintColumnsPort.deleteByColumnId(columnId),
        deleteIndexColumnsPort.deleteByColumnId(columnId),
        deleteRelationshipColumnsPort.deleteByColumnId(columnId)).then(deleteColumnPort.deleteColumn(columnId));
  }

}
