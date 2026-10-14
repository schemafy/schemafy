package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipExtraInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import reactor.core.publisher.Mono;

@Component
class ChangeRelationshipExtraUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeRelationshipExtraInverse> {

  private final ChangeRelationshipExtraPort changeRelationshipExtraPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  ChangeRelationshipExtraUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeRelationshipExtraPort changeRelationshipExtraPort,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    super(ErdOperationType.CHANGE_RELATIONSHIP_EXTRA, ChangeRelationshipExtraInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeRelationshipExtraPort = changeRelationshipExtraPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeRelationshipExtraInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getRelationshipByIdPort.findRelationshipById(inversePayload.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            "Relationship not found: " + inversePayload.relationshipId())))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          return coordinate(resolved, inversePayload,
              () -> changeRelationshipExtraPort.changeRelationshipExtra(
                  inversePayload.relationshipId(),
                  inversePayload.oldExtra())
                  .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                      .withInverse(new ChangeRelationshipExtraInverse(
                          relationship.id(),
                          relationship.extra()))));
        });
  }

}
