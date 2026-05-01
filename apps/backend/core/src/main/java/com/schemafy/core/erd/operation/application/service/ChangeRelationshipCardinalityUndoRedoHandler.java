package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipCardinalityInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipCardinalityPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import reactor.core.publisher.Mono;
@Component
class ChangeRelationshipCardinalityUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeRelationshipCardinalityInverse> {

  private final ChangeRelationshipCardinalityPort changeRelationshipCardinalityPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  ChangeRelationshipCardinalityUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeRelationshipCardinalityPort changeRelationshipCardinalityPort,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    super(ErdOperationType.CHANGE_RELATIONSHIP_CARDINALITY, ChangeRelationshipCardinalityInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeRelationshipCardinalityPort = changeRelationshipCardinalityPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeRelationshipCardinalityInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getRelationshipByIdPort.findRelationshipById(inversePayload.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            "Relationship not found: " + inversePayload.relationshipId())))
        .flatMap(relationship -> coordinate(resolved, inversePayload,
            () -> {
              Set<String> affectedTableIds = new HashSet<>();
              affectedTableIds.add(relationship.fkTableId());
              affectedTableIds.add(relationship.pkTableId());
              return changeRelationshipCardinalityPort.changeRelationshipCardinality(
                  inversePayload.relationshipId(),
                  inversePayload.oldCardinality())
                  .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                      .withInverse(new ChangeRelationshipCardinalityInverse(
                          relationship.id(),
                          relationship.cardinality())));
            }));
  }

}
