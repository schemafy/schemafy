package com.schemafy.core.erd.table.application.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.core.erd.table.application.port.out.DeleteTablePort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

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
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> deleteTable(DeleteTableCommand command) {
    String tableId = command.tableId();
    Set<String> affectedTableIds = ConcurrentHashMap.newKeySet();
    affectedTableIds.add(tableId);
    Mono<Void> ensureExists = getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + tableId)))
        .then();
    return erdMutationCoordinator.coordinate(ErdOperationType.DELETE_TABLE, command, () -> ensureExists
        .then(getRelationshipsByTableIdPort.findRelationshipsByTableId(tableId)
            .defaultIfEmpty(List.of())
            .flatMapMany(Flux::fromIterable)
            // Identifying cascade can remove later relationships from the initial snapshot.
            .concatMap(relationship -> deleteRelationshipIgnoringAlreadyDeleted(relationship.id(), affectedTableIds))
            .then(Mono.defer(() -> getColumnsByTableIdPort.findColumnsByTableId(tableId)))
            .flatMapMany(Flux::fromIterable)
            .concatMap(column -> deleteColumnUseCase.deleteColumn(
                new DeleteColumnCommand(column.id()))
                .contextWrite(ErdOperationContexts.suppressNestedMutation())
                .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                .then())
            .then(Mono.defer(() -> deleteTablePort.deleteTable(tableId)))
            .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)))))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> deleteRelationshipIgnoringAlreadyDeleted(
      String relationshipId,
      Set<String> affectedTableIds) {
    return deleteRelationshipUseCase.deleteRelationship(new DeleteRelationshipCommand(relationshipId))
        .contextWrite(ErdOperationContexts.suppressNestedMutation())
        .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
        .then()
        .onErrorResume(DomainException.class, ex -> ex.getErrorCode() == RelationshipErrorCode.NOT_FOUND
            ? Mono.empty()
            : Mono.error(ex));
  }

}
