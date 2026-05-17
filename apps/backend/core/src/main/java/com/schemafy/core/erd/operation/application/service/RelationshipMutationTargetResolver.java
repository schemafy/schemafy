package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipCardinalityInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.requirePayload;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.resolveStructuralOr;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.unsupportedTargetOperation;

@Component
@RequiredArgsConstructor
class RelationshipMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolve(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CREATE_RELATIONSHIP -> resolveCreateRelationship(payload);
    case CHANGE_RELATIONSHIP_NAME -> resolveChangeRelationshipName(payload);
    case CHANGE_RELATIONSHIP_KIND -> resolveChangeRelationshipKind(payload);
    case CHANGE_RELATIONSHIP_CARDINALITY -> resolveChangeRelationshipCardinality(payload);
    case CHANGE_RELATIONSHIP_EXTRA -> resolveChangeRelationshipExtra(payload);
    case DELETE_RELATIONSHIP -> resolveDeleteRelationship(payload);
    case ADD_RELATIONSHIP_COLUMN -> resolveAddRelationshipColumn(payload);
    case REMOVE_RELATIONSHIP_COLUMN -> resolveRemoveRelationshipColumn(payload);
    case CHANGE_RELATIONSHIP_COLUMN_POSITION -> resolveChangeRelationshipColumnPosition(payload);
    default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveCreateRelationship(Object payload) {
    CreateRelationshipCommand command = requirePayload(payload, CreateRelationshipCommand.class);
    return targetLookup.resolveTableContext(command.fkTableId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipName(Object payload) {
    if (payload instanceof ChangeRelationshipNameInverse inverse) {
      return targetLookup.resolveByRelationshipId(inverse.relationshipId(), inverse.relationshipId());
    }
    ChangeRelationshipNameCommand command = requirePayload(payload, ChangeRelationshipNameCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipKind(Object payload) {
    ChangeRelationshipKindCommand command = requirePayload(payload, ChangeRelationshipKindCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipCardinality(Object payload) {
    if (payload instanceof ChangeRelationshipCardinalityInverse inverse) {
      return targetLookup.resolveByRelationshipId(inverse.relationshipId(), inverse.relationshipId());
    }
    ChangeRelationshipCardinalityCommand command = requirePayload(
        payload, ChangeRelationshipCardinalityCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipExtra(Object payload) {
    ChangeRelationshipExtraCommand command = requirePayload(payload, ChangeRelationshipExtraCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveDeleteRelationship(Object payload) {
    DeleteRelationshipCommand command = requirePayload(payload, DeleteRelationshipCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveAddRelationshipColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      AddRelationshipColumnCommand command = requirePayload(payload, AddRelationshipColumnCommand.class);
      return targetLookup.resolveByRelationshipId(command.relationshipId(), null);
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveRemoveRelationshipColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      RemoveRelationshipColumnCommand command = requirePayload(payload, RemoveRelationshipColumnCommand.class);
      return targetLookup.resolveByRelationshipColumnId(
          command.relationshipColumnId(), command.relationshipColumnId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipColumnPosition(Object payload) {
    ChangeRelationshipColumnPositionCommand command = requirePayload(
        payload, ChangeRelationshipColumnPositionCommand.class);
    return targetLookup.resolveByRelationshipColumnId(
        command.relationshipColumnId(), command.relationshipColumnId());
  }

}
