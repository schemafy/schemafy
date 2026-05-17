package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.requirePayload;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.resolveStructuralOr;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.unsupportedTargetOperation;

@Component
@RequiredArgsConstructor
class ConstraintMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolve(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CREATE_CONSTRAINT -> resolveCreateConstraint(payload);
    case CHANGE_CONSTRAINT_NAME -> resolveChangeConstraintName(payload);
    case CHANGE_CONSTRAINT_CHECK_EXPR -> resolveChangeConstraintCheckExpr(payload);
    case CHANGE_CONSTRAINT_DEFAULT_EXPR -> resolveChangeConstraintDefaultExpr(payload);
    case DELETE_CONSTRAINT -> resolveDeleteConstraint(payload);
    case ADD_CONSTRAINT_COLUMN -> resolveAddConstraintColumn(payload);
    case REMOVE_CONSTRAINT_COLUMN -> resolveRemoveConstraintColumn(payload);
    case CHANGE_CONSTRAINT_COLUMN_POSITION -> resolveChangeConstraintColumnPosition(payload);
    default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveCreateConstraint(Object payload) {
    CreateConstraintCommand command = requirePayload(payload, CreateConstraintCommand.class);
    return targetLookup.resolveTableContext(command.tableId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeConstraintName(Object payload) {
    if (payload instanceof ChangeConstraintNameInverse inverse) {
      return targetLookup.resolveByConstraintId(inverse.constraintId(), inverse.constraintId());
    }
    ChangeConstraintNameCommand command = requirePayload(payload, ChangeConstraintNameCommand.class);
    return targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeConstraintCheckExpr(Object payload) {
    ChangeConstraintCheckExprCommand command = requirePayload(payload, ChangeConstraintCheckExprCommand.class);
    return targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeConstraintDefaultExpr(Object payload) {
    ChangeConstraintDefaultExprCommand command = requirePayload(payload, ChangeConstraintDefaultExprCommand.class);
    return targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
  }

  private Mono<ResolvedErdMutationTarget> resolveDeleteConstraint(Object payload) {
    DeleteConstraintCommand command = requirePayload(payload, DeleteConstraintCommand.class);
    return targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
  }

  private Mono<ResolvedErdMutationTarget> resolveAddConstraintColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      AddConstraintColumnCommand command = requirePayload(payload, AddConstraintColumnCommand.class);
      return targetLookup.resolveByConstraintId(command.constraintId(), null);
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveRemoveConstraintColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      RemoveConstraintColumnCommand command = requirePayload(payload, RemoveConstraintColumnCommand.class);
      return targetLookup.resolveByConstraintColumnId(command.constraintColumnId(), command.constraintColumnId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeConstraintColumnPosition(Object payload) {
    ChangeConstraintColumnPositionCommand command = requirePayload(
        payload, ChangeConstraintColumnPositionCommand.class);
    return targetLookup.resolveByConstraintColumnId(command.constraintColumnId(), command.constraintColumnId());
  }

}
