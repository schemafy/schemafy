package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.requirePayload;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.resolveStructuralOr;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.unsupportedTargetOperation;

@Component
@RequiredArgsConstructor
class ColumnMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolve(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CREATE_COLUMN -> resolveCreateColumn(payload);
    case CHANGE_COLUMN_NAME -> resolveChangeColumnName(payload);
    case CHANGE_COLUMN_TYPE -> resolveChangeColumnType(payload);
    case CHANGE_COLUMN_META -> resolveChangeColumnMeta(payload);
    case CHANGE_COLUMN_POSITION -> resolveChangeColumnPosition(payload);
    case DELETE_COLUMN -> resolveDeleteColumn(payload);
    default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveCreateColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      CreateColumnCommand command = requirePayload(payload, CreateColumnCommand.class);
      return targetLookup.resolveTableContext(command.tableId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeColumnName(Object payload) {
    if (payload instanceof ChangeColumnNameInverse inverse) {
      return targetLookup.resolveByColumnId(inverse.columnId(), inverse.columnId());
    }
    ChangeColumnNameCommand command = requirePayload(payload, ChangeColumnNameCommand.class);
    return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeColumnType(Object payload) {
    if (payload instanceof ChangeColumnTypeInverse inverse) {
      return targetLookup.resolveByColumnId(inverse.columnId(), inverse.columnId());
    }
    ChangeColumnTypeCommand command = requirePayload(payload, ChangeColumnTypeCommand.class);
    return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeColumnMeta(Object payload) {
    ChangeColumnMetaCommand command = requirePayload(payload, ChangeColumnMetaCommand.class);
    return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeColumnPosition(Object payload) {
    ChangeColumnPositionCommand command = requirePayload(payload, ChangeColumnPositionCommand.class);
    return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveDeleteColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      DeleteColumnCommand command = requirePayload(payload, DeleteColumnCommand.class);
      return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
    });
  }

}
