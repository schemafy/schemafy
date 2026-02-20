package com.schemafy.domain.erd.column.application.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.DeleteColumnPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByColumnIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteColumnService implements DeleteColumnUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteColumnPort deleteColumnPort;
  private final GetColumnByIdPort getColumnByIdPort;

  private final GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final DeleteConstraintColumnsByColumnIdPort deleteConstraintColumnsPort;
  private final DeleteConstraintPort deleteConstraintPort;
  private final GetConstraintByIdPort getConstraintByIdPort;

  private final GetIndexColumnsByColumnIdPort getIndexColumnsByColumnIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final DeleteIndexColumnsByColumnIdPort deleteIndexColumnsPort;
  private final DeleteIndexPort deleteIndexPort;

  private final GetRelationshipColumnsByColumnIdPort getRelationshipColumnsByColumnIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  private final DeleteRelationshipColumnsByColumnIdPort deleteRelationshipColumnsPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort;
  private final DeleteRelationshipPort deleteRelationshipPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;

  @Override
  public Mono<MutationResult<Void>> deleteColumn(DeleteColumnCommand command) {
    Set<String> affectedTableIds = ConcurrentHashMap.newKeySet();
    return rejectIfForeignKeyColumn(command.columnId())
        .then(Mono.defer(() -> deleteColumnInternal(
            command.columnId(),
            ConcurrentHashMap.newKeySet(),
            affectedTableIds)))
        .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> rejectIfForeignKeyColumn(String columnId) {
    return getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(columnId)
        .flatMap(relationshipColumns -> {
          boolean isFk = relationshipColumns.stream()
              .anyMatch(rc -> rc.fkColumnId().equals(columnId));
          if (isFk) {
            return Mono.error(new DomainException(ColumnErrorCode.FK_PROTECTED,
                "Foreign key columns cannot be deleted directly"));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> deleteColumnInternal(
      String columnId,
      Set<String> visitedColumnIds,
      Set<String> affectedTableIds) {
    if (!visitedColumnIds.add(columnId)) {
      return Mono.empty();
    }

    Mono<Void> trackTableId = getColumnByIdPort.findColumnById(columnId)
        .switchIfEmpty(Mono.error(
            new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + columnId)))
        .doOnNext(column -> affectedTableIds.add(column.tableId()))
        .then();

    return trackTableId
        .then(cascadeDeleteConstraints(columnId, visitedColumnIds, affectedTableIds))
        .then(cascadeDeleteIndexes(columnId))
        .then(cascadeDeleteRelationships(columnId, affectedTableIds))
        .then(deleteColumnPort.deleteColumn(columnId));
  }

  private Mono<Void> cascadeDeleteConstraints(
      String columnId,
      Set<String> visitedColumnIds,
      Set<String> affectedTableIds) {
    return getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(columnId)
        .flatMap(constraintColumns -> {
          if (constraintColumns.isEmpty()) {
            return Mono.empty();
          }

          Set<String> affectedConstraintIds = constraintColumns.stream()
              .map(ConstraintColumn::constraintId)
              .collect(Collectors.toSet());

          return handlePkConstraintCascade(
              affectedConstraintIds,
              columnId,
              visitedColumnIds,
              affectedTableIds)
              .then(deleteConstraintColumnsPort.deleteByColumnId(columnId))
              .then(deleteOrphanConstraints(affectedConstraintIds));
        });
  }

  private Mono<Void> handlePkConstraintCascade(
      Set<String> constraintIds,
      String columnId,
      Set<String> visitedColumnIds,
      Set<String> affectedTableIds) {
    return Flux.fromIterable(constraintIds)
        .concatMap(getConstraintByIdPort::findConstraintById)
        .filter(constraint -> constraint.kind() == ConstraintKind.PRIMARY_KEY)
        .concatMap(constraint -> cascadeDeleteRelationshipsByPkColumn(
            constraint,
            columnId,
            visitedColumnIds,
            affectedTableIds))
        .then();
  }

  private Mono<Void> cascadeDeleteRelationshipsByPkColumn(
      Constraint pkConstraint,
      String pkColumnId,
      Set<String> visitedColumnIds,
      Set<String> affectedTableIds) {
    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkConstraint.tableId())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> cascadeDeleteRelationshipColumnsByPkColumnId(relationship, pkColumnId,
            visitedColumnIds,
            affectedTableIds))
        .then();
  }

  private Mono<Void> cascadeDeleteRelationshipColumnsByPkColumnId(
      Relationship relationship,
      String pkColumnId,
      Set<String> visitedColumnIds,
      Set<String> affectedTableIds) {
    affectedTableIds.add(relationship.fkTableId());
    affectedTableIds.add(relationship.pkTableId());
    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationship.id())
        .flatMap(columns -> {
          List<RelationshipColumn> toRemove = columns.stream()
              .filter(col -> col.pkColumnId().equals(pkColumnId))
              .toList();

          if (toRemove.isEmpty()) {
            return Mono.empty();
          }

          List<String> fkColumnIds = toRemove.stream()
              .map(RelationshipColumn::fkColumnId)
              .toList();

          List<RelationshipColumn> remaining = columns.stream()
              .filter(col -> !col.pkColumnId().equals(pkColumnId))
              .toList();

          Mono<Void> deleteRelationshipColumns;
          if (remaining.isEmpty()) {
            deleteRelationshipColumns = deleteRelationshipColumnsByRelationshipIdPort
                .deleteByRelationshipId(relationship.id())
                .then(deleteRelationshipPort.deleteRelationship(relationship.id()));
          } else {
            deleteRelationshipColumns = Flux.fromIterable(toRemove)
                .concatMap(col -> deleteRelationshipColumnPort.deleteRelationshipColumn(col.id()))
                .then();
          }

          return deleteRelationshipColumns
              .thenMany(Flux.fromIterable(fkColumnIds))
              .concatMap(fkColumnId -> deleteColumnInternal(
                  fkColumnId,
                  visitedColumnIds,
                  affectedTableIds))
              .then();
        });
  }

  private Mono<Void> deleteOrphanConstraints(Set<String> constraintIds) {
    return Flux.fromIterable(constraintIds)
        .concatMap(constraintId -> getConstraintColumnsByConstraintIdPort
            .findConstraintColumnsByConstraintId(constraintId)
            .flatMap(columns -> {
              if (columns.isEmpty()) {
                return deleteConstraintPort.deleteConstraint(constraintId);
              }
              return Mono.empty();
            }))
        .then();
  }

  private Mono<Void> cascadeDeleteIndexes(String columnId) {
    return getIndexColumnsByColumnIdPort.findIndexColumnsByColumnId(columnId)
        .flatMap(indexColumns -> {
          if (indexColumns.isEmpty()) {
            return Mono.empty();
          }

          Set<String> affectedIndexIds = indexColumns.stream()
              .map(IndexColumn::indexId)
              .collect(Collectors.toSet());

          return deleteIndexColumnsPort.deleteByColumnId(columnId)
              .then(deleteOrphanIndexes(affectedIndexIds));
        });
  }

  private Mono<Void> deleteOrphanIndexes(Set<String> indexIds) {
    return Flux.fromIterable(indexIds)
        .concatMap(indexId -> getIndexColumnsByIndexIdPort
            .findIndexColumnsByIndexId(indexId)
            .flatMap(columns -> {
              if (columns.isEmpty()) {
                return deleteIndexPort.deleteIndex(indexId);
              }
              return Mono.empty();
            }))
        .then();
  }

  private Mono<Void> cascadeDeleteRelationships(String columnId, Set<String> affectedTableIds) {
    return getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(columnId)
        .flatMap(relationshipColumns -> {
          if (relationshipColumns.isEmpty()) {
            return Mono.empty();
          }

          Set<String> affectedRelationshipIds = relationshipColumns.stream()
              .map(RelationshipColumn::relationshipId)
              .collect(Collectors.toSet());

          return deleteRelationshipColumnsPort.deleteByColumnId(columnId)
              .then(deleteOrphanRelationships(affectedRelationshipIds, affectedTableIds));
        });
  }

  private Mono<Void> deleteOrphanRelationships(
      Set<String> relationshipIds,
      Set<String> affectedTableIds) {
    return Flux.fromIterable(relationshipIds)
        .concatMap(relationshipId -> getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationshipId)
            .flatMap(columns -> {
              if (columns.isEmpty()) {
                return getRelationshipByIdPort.findRelationshipById(relationshipId)
                    .doOnNext(rel -> {
                      affectedTableIds.add(rel.fkTableId());
                      affectedTableIds.add(rel.pkTableId());
                    })
                    .then(deleteRelationshipPort.deleteRelationship(relationshipId));
              }
              return Mono.empty();
            }))
        .then();
  }

}
