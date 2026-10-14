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
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ReorderPositions;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipColumnPositionUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.RELATIONSHIP_COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = RELATIONSHIP_COLUMN, id = "relationshipColumnId"))
public class ChangeRelationshipColumnPositionService
    implements ChangeRelationshipColumnPositionUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeRelationshipColumnPosition(
      ChangeRelationshipColumnPositionCommand command) {
    return getRelationshipColumnByIdPort.findRelationshipColumnById(command.relationshipColumnId())
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.POSITION_INVALID,
            "Relationship column not found")))
        .flatMap(relationshipColumn -> getRelationshipByIdPort
            .findRelationshipById(relationshipColumn.relationshipId())
            .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND,
                "Relationship not found")))
            .flatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
                .findRelationshipColumnsByRelationshipId(relationshipColumn.relationshipId())
                .defaultIfEmpty(List.of())
                .flatMap(columns -> {
                  Set<String> affectedTableIds = toTableIdSet(relationship);
                  int currentPosition = resolveCurrentPosition(relationshipColumn, columns);
                  int normalizedPosition = Math.clamp(command.seqNo(), 0, columns.size() - 1);
                  if (currentPosition == normalizedPosition) {
                    return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
                  }
                  return erdMutationCoordinator.coordinate(
                      ErdOperationType.CHANGE_RELATIONSHIP_COLUMN_POSITION,
                      command,
                      () -> getRelationshipColumnsByRelationshipIdPort
                          .findRelationshipColumnsByRelationshipId(relationshipColumn.relationshipId())
                          .defaultIfEmpty(List.of())
                          .flatMap(lockedColumns -> {
                            int lockedCurrentPosition = resolveCurrentPosition(
                                relationshipColumn,
                                lockedColumns);
                            int lockedNormalizedPosition = Math.clamp(
                                command.seqNo(),
                                0,
                                lockedColumns.size() - 1);
                            if (lockedCurrentPosition == lockedNormalizedPosition) {
                              return Mono.just(MutationResult.<Void>noop(null,
                                  affectedTableIds));
                            }
                            List<RelationshipColumn> reordered = reorderColumns(
                                lockedColumns,
                                lockedCurrentPosition,
                                lockedNormalizedPosition);
                            return changeRelationshipColumnPositionPort
                                .changeRelationshipColumnPositions(
                                    relationshipColumn.relationshipId(), reordered)
                                .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                                    .withInverse(new ChangeRelationshipColumnPositionInverse(
                                        relationshipColumn.id(),
                                        ReorderPositions.capture(
                                            lockedColumns,
                                            RelationshipColumn::id,
                                            RelationshipColumn::seqNo))));
                          }));
                })))
        .as(transactionalOperator::transactional);
  }

  private static Set<String> toTableIdSet(Relationship relationship) {
    Set<String> affectedTableIds = new HashSet<>();
    affectedTableIds.add(relationship.fkTableId());
    affectedTableIds.add(relationship.pkTableId());
    return affectedTableIds;
  }

  private int resolveCurrentPosition(
      RelationshipColumn relationshipColumn,
      List<RelationshipColumn> columns) {
    if (columns.isEmpty()) {
      throw new DomainException(RelationshipErrorCode.POSITION_INVALID,
          "Relationship column not found");
    }
    int currentPosition = findIndex(columns, relationshipColumn.id());
    if (currentPosition < 0) {
      throw new DomainException(RelationshipErrorCode.POSITION_INVALID,
          "Relationship column not found");
    }
    return currentPosition;
  }

  private List<RelationshipColumn> reorderColumns(
      List<RelationshipColumn> columns,
      int currentIndex,
      int normalizedPosition) {
    List<RelationshipColumn> reordered = new ArrayList<>(columns);
    RelationshipColumn movingColumn = reordered.remove(currentIndex);
    reordered.add(normalizedPosition, movingColumn);

    List<RelationshipColumn> updated = new ArrayList<>(reordered.size());
    for (int index = 0; index < reordered.size(); index++) {
      RelationshipColumn column = reordered.get(index);
      updated.add(column.withSeqNo(index));
    }

    return updated;
  }

  private static int findIndex(List<RelationshipColumn> columns, String relationshipColumnId) {
    for (int index = 0; index < columns.size(); index++) {
      if (equalsIgnoreCase(columns.get(index).id(), relationshipColumnId)) {
        return index;
      }
    }
    return -1;
  }

  private static boolean equalsIgnoreCase(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equalsIgnoreCase(right);
  }

}
