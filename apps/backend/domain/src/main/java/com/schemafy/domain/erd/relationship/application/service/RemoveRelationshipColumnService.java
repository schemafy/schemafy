package com.schemafy.domain.erd.relationship.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RemoveRelationshipColumnService implements RemoveRelationshipColumnUseCase {

  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final TransactionalOperator transactionalOperator;
  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteColumnUseCase deleteColumnUseCase;

  @Override
  public Mono<MutationResult<Void>> removeRelationshipColumn(RemoveRelationshipColumnCommand command) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          return getRelationshipColumnByIdPort
              .findRelationshipColumnById(command.relationshipColumnId())
              .switchIfEmpty(Mono.error(new RelationshipColumnNotExistException(
                  "Relationship column not found")))
              .flatMap(relationshipColumn -> {
                if (!relationshipColumn.relationshipId().equalsIgnoreCase(relationship.id())) {
                  return Mono.error(new RelationshipColumnNotExistException(
                      "Relationship column not found"));
                }
                String fkColumnId = relationshipColumn.fkColumnId();
                return deleteRelationshipColumnPort
                    .deleteRelationshipColumn(relationshipColumn.id())
                    .then(handleRemainingColumns(relationship.id()))
                    .then(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(fkColumnId)))
                    .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
                    .thenReturn(MutationResult.<Void>of(null, affectedTableIds));
              });
        })
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
