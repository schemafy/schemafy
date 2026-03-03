package com.schemafy.domain.erd.table.application.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteTableService implements DeleteTableUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteTablePort deleteTablePort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;
  private final DeleteRelationshipUseCase deleteRelationshipUseCase;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;

  @Override
  public Mono<MutationResult<Void>> deleteTable(DeleteTableCommand command) {
    String tableId = command.tableId();
    Set<String> affectedTableIds = ConcurrentHashMap.newKeySet();
    affectedTableIds.add(tableId);
    Mono<Void> ensureExists = getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + tableId)))
        .then();
    return ensureExists
        .then(getRelationshipsByTableIdPort.findRelationshipsByTableId(tableId)
            .defaultIfEmpty(List.of())
            .flatMapMany(Flux::fromIterable)
            .concatMap(relationship -> deleteRelationshipUseCase.deleteRelationship(
                new DeleteRelationshipCommand(relationship.id()))
                .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                .then())
            .then(Mono.defer(() -> getColumnsByTableIdPort.findColumnsByTableId(tableId)))
            .flatMapMany(Flux::fromIterable)
            .concatMap(column -> deleteColumnUseCase.deleteColumn(
                new DeleteColumnCommand(column.id()))
                .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                .then())
            .then(Mono.defer(() -> deleteTablePort.deleteTable(tableId)))
            .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds))))
        .as(transactionalOperator::transactional);
  }

}
