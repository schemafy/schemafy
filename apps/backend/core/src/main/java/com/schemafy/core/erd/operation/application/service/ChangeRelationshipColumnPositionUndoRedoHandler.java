package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ReorderPositions;
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
                            relationship.id(),
                            ReorderPositions.restore(
                                columns,
                                RelationshipColumn::id,
                                inversePayload.positions(),
                                ChangeRelationshipColumnPositionUndoRedoHandler::withSeqNo))
                        .thenReturn(MutationResult.<Void>of(null, affectedTableIds(relationship))
                            .withInverse(new ChangeRelationshipColumnPositionInverse(
                                relationshipColumn.id(),
                                ReorderPositions.capture(
                                    columns,
                                    RelationshipColumn::id,
                                    RelationshipColumn::seqNo))))))));
  }

  private static RelationshipColumn withSeqNo(
      RelationshipColumn column,
      int seqNo) {
    return new RelationshipColumn(
        column.id(),
        column.relationshipId(),
        column.pkColumnId(),
        column.fkColumnId(),
        seqNo);
  }

  private static Set<String> affectedTableIds(Relationship relationship) {
    Set<String> affectedTableIds = new HashSet<>();
    affectedTableIds.add(relationship.fkTableId());
    affectedTableIds.add(relationship.pkTableId());
    return affectedTableIds;
  }

}
