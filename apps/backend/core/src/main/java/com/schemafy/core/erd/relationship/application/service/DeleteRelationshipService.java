package com.schemafy.core.erd.relationship.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteRelationshipService implements DeleteRelationshipUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> deleteRelationship(DeleteRelationshipCommand command) {
    String relationshipId = command.relationshipId();

    return erdMutationCoordinator.coordinate(ErdOperationType.DELETE_RELATIONSHIP, command,
        () -> getRelationshipByIdPort.findRelationshipById(relationshipId)
            .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found")))
            .flatMap(relationship -> {
              Set<String> affectedTableIds = new HashSet<>();
              affectedTableIds.add(relationship.fkTableId());
              affectedTableIds.add(relationship.pkTableId());
              return getRelationshipColumnsByRelationshipIdPort
                  .findRelationshipColumnsByRelationshipId(relationshipId)
                  .defaultIfEmpty(List.of())
                  .flatMap(relColumns -> {
                    List<String> fkColumnIds = relColumns.stream()
                        .map(RelationshipColumn::fkColumnId)
                        .toList();

                    return deleteRelationshipColumnsPort.deleteByRelationshipId(relationshipId)
                        .then(deleteRelationshipPort.deleteRelationship(relationshipId))
                        .thenMany(Flux.fromIterable(fkColumnIds))
                        .concatMap(fkColumnId -> deleteColumnUseCase.deleteColumn(
                            new DeleteColumnCommand(fkColumnId))
                            .contextWrite(ErdOperationContexts.suppressNestedMutation())
                            .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                            .then())
                        .then()
                        .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
                  });
            }))
        .as(transactionalOperator::transactional);
  }

}
