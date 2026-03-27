package com.schemafy.core.erd.relationship.application.service;

import java.util.ArrayList;
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
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RemoveRelationshipColumnService implements RemoveRelationshipColumnUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> removeRelationshipColumn(RemoveRelationshipColumnCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.REMOVE_RELATIONSHIP_COLUMN, command,
        () -> getRelationshipColumnByIdPort
        .findRelationshipColumnById(command.relationshipColumnId())
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.COLUMN_NOT_FOUND,
            "Relationship column not found")))
        .flatMap(relationshipColumn -> getRelationshipByIdPort
            .findRelationshipById(relationshipColumn.relationshipId())
            .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found")))
            .flatMap(relationship -> {
              Set<String> affectedTableIds = new HashSet<>();
              affectedTableIds.add(relationship.fkTableId());
              affectedTableIds.add(relationship.pkTableId());
              String fkColumnId = relationshipColumn.fkColumnId();
              return deleteRelationshipColumnPort
                  .deleteRelationshipColumn(relationshipColumn.id())
                  .then(handleRemainingColumns(relationship.id()))
                  .then(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(fkColumnId))
                      .contextWrite(ErdOperationContexts.suppressNestedMutation()))
                  .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                  .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
	            }))
	        )
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> handleRemainingColumns(String relationshipId) {
    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationshipId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return deleteRelationshipPort.deleteRelationship(relationshipId);
          }
          List<RelationshipColumn> reordered = new ArrayList<>(columns.size());
          for (int index = 0; index < columns.size(); index++) {
            RelationshipColumn column = columns.get(index);
            reordered.add(new RelationshipColumn(
                column.id(),
                column.relationshipId(),
                column.pkColumnId(),
                column.fkColumnId(),
                index));
          }
          return changeRelationshipColumnPositionPort
              .changeRelationshipColumnPositions(relationshipId, reordered);
        });
  }

}
