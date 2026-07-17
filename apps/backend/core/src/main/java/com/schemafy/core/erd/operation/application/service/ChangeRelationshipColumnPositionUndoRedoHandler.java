package com.schemafy.core.erd.operation.application.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ReorderPosition;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import reactor.core.publisher.Mono;

@Component
class ChangeRelationshipColumnPositionUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeRelationshipColumnPositionInverse> {

  private final ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  ChangeRelationshipColumnPositionUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeRelationshipColumnPositionPort changeRelationshipColumnPositionPort,
      GetRelationshipByIdPort getRelationshipByIdPort,
      GetRelationshipColumnByIdPort getRelationshipColumnByIdPort,
      GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort) {
    super(ErdOperationType.CHANGE_RELATIONSHIP_COLUMN_POSITION,
        ChangeRelationshipColumnPositionInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeRelationshipColumnPositionPort = changeRelationshipColumnPositionPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
    this.getRelationshipColumnByIdPort = getRelationshipColumnByIdPort;
    this.getRelationshipColumnsByRelationshipIdPort = getRelationshipColumnsByRelationshipIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeRelationshipColumnPositionInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return coordinate(resolved, inversePayload,
        () -> getRelationshipColumnByIdPort
            .findRelationshipColumnById(inversePayload.relationshipColumnId())
            .switchIfEmpty(Mono.error(new DomainException(
                RelationshipErrorCode.POSITION_INVALID,
                "Relationship column not found: " + inversePayload.relationshipColumnId())))
            .flatMap(relationshipColumn -> getRelationshipByIdPort
                .findRelationshipById(relationshipColumn.relationshipId())
                .switchIfEmpty(Mono.error(new DomainException(
                    RelationshipErrorCode.NOT_FOUND,
                    "Relationship not found: " + relationshipColumn.relationshipId())))
                .flatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
                    .findRelationshipColumnsByRelationshipId(relationship.id())
                    .defaultIfEmpty(List.of())
                    .flatMap(columns -> changeRelationshipColumnPositionPort
                        .changeRelationshipColumnPositions(
                            relationship.id(), restorePositions(columns, inversePayload.positions()))
                        .thenReturn(MutationResult.<Void>of(null, affectedTableIds(relationship))
                            .withInverse(new ChangeRelationshipColumnPositionInverse(
                                relationshipColumn.id(),
                                toPositions(columns))))))));
  }

  private static List<RelationshipColumn> restorePositions(
      List<RelationshipColumn> columns,
      List<ReorderPosition> positions) {
    Map<String, Integer> positionsById = positionsById(columns.size(), positions);
    return columns.stream()
        .map(column -> new RelationshipColumn(
            column.id(),
            column.relationshipId(),
            column.pkColumnId(),
            column.fkColumnId(),
            requirePosition(positionsById, column.id())))
        .toList();
  }

  private static List<ReorderPosition> toPositions(List<RelationshipColumn> columns) {
    return columns.stream()
        .map(column -> new ReorderPosition(column.id(), column.seqNo()))
        .toList();
  }

  private static Set<String> affectedTableIds(Relationship relationship) {
    Set<String> affectedTableIds = new HashSet<>();
    affectedTableIds.add(relationship.fkTableId());
    affectedTableIds.add(relationship.pkTableId());
    return affectedTableIds;
  }

  private static Map<String, Integer> positionsById(
      int currentSize,
      List<ReorderPosition> positions) {
    if (positions.size() != currentSize) {
      throw snapshotMismatch();
    }
    Map<String, Integer> positionsById = new HashMap<>(positions.size());
    for (ReorderPosition position : positions) {
      if (positionsById.put(position.entityId(), position.seqNo()) != null) {
        throw snapshotMismatch();
      }
    }
    return positionsById;
  }

  private static int requirePosition(Map<String, Integer> positionsById, String entityId) {
    Integer position = positionsById.get(entityId);
    if (position == null) {
      throw snapshotMismatch();
    }
    return position;
  }

  private static IllegalStateException snapshotMismatch() {
    return new IllegalStateException(
        "Relationship column reorder snapshot does not match current columns");
  }

}
