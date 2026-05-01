package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import reactor.core.publisher.Mono;

@Component
class ChangeRelationshipNameUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeRelationshipNameInverse> {

  private final ChangeRelationshipNamePort changeRelationshipNamePort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  ChangeRelationshipNameUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeRelationshipNamePort changeRelationshipNamePort,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    super(ErdOperationType.CHANGE_RELATIONSHIP_NAME, ChangeRelationshipNameInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeRelationshipNamePort = changeRelationshipNamePort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeRelationshipNameInverse inversePayload,
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
              return changeRelationshipNamePort.changeRelationshipName(
                  inversePayload.relationshipId(),
                  inversePayload.oldName())
                  .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                      .withInverse(new ChangeRelationshipNameInverse(
                          relationship.id(),
                          relationship.name())));
            }));
  }

}
