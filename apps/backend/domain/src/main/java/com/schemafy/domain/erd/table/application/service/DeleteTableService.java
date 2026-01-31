package com.schemafy.domain.erd.table.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteTableService implements DeleteTableUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteTablePort deleteTablePort;
  private final GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;
  private final DeleteRelationshipUseCase deleteRelationshipUseCase;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;

  @Override
  public Mono<Void> deleteTable(DeleteTableCommand command) {
    String tableId = command.tableId();
    return getRelationshipsByTableIdPort.findRelationshipsByTableId(tableId)
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> deleteRelationshipUseCase.deleteRelationship(
            new DeleteRelationshipCommand(relationship.id())))
        .then(Mono.defer(() -> getColumnsByTableIdPort.findColumnsByTableId(tableId)))
        .flatMapMany(Flux::fromIterable)
        .concatMap(column -> deleteColumnUseCase.deleteColumn(
            new DeleteColumnCommand(column.id())))
        .then(Mono.defer(() -> deleteTablePort.deleteTable(tableId)))
        .as(transactionalOperator::transactional);
  }

}
