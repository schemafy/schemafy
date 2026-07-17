package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexTypeInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.requirePayload;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.resolveStructuralOr;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.unsupportedTargetOperation;

@Component
@RequiredArgsConstructor
class IndexMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolve(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CREATE_INDEX -> resolveCreateIndex(payload);
    case CHANGE_INDEX_NAME -> resolveChangeIndexName(payload);
    case CHANGE_INDEX_TYPE -> resolveChangeIndexType(payload);
    case DELETE_INDEX -> resolveDeleteIndex(payload);
    case ADD_INDEX_COLUMN -> resolveAddIndexColumn(payload);
    case REMOVE_INDEX_COLUMN -> resolveRemoveIndexColumn(payload);
    case CHANGE_INDEX_COLUMN_POSITION -> resolveChangeIndexColumnPosition(payload);
    case CHANGE_INDEX_COLUMN_SORT_DIRECTION -> resolveChangeIndexColumnSortDirection(payload);
    default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveCreateIndex(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      CreateIndexCommand command = requirePayload(payload, CreateIndexCommand.class);
      return targetLookup.resolveTableContext(command.tableId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeIndexName(Object payload) {
    if (payload instanceof ChangeIndexNameInverse inverse) {
      return targetLookup.resolveByIndexId(inverse.indexId(), inverse.indexId());
    }
    ChangeIndexNameCommand command = requirePayload(payload, ChangeIndexNameCommand.class);
    return targetLookup.resolveByIndexId(command.indexId(), command.indexId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeIndexType(Object payload) {
    if (payload instanceof ChangeIndexTypeInverse inverse) {
      return targetLookup.resolveByIndexId(inverse.indexId(), inverse.indexId());
    }
    ChangeIndexTypeCommand command = requirePayload(payload, ChangeIndexTypeCommand.class);
    return targetLookup.resolveByIndexId(command.indexId(), command.indexId());
  }

  private Mono<ResolvedErdMutationTarget> resolveDeleteIndex(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      DeleteIndexCommand command = requirePayload(payload, DeleteIndexCommand.class);
      return targetLookup.resolveByIndexId(command.indexId(), command.indexId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveAddIndexColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      AddIndexColumnCommand command = requirePayload(payload, AddIndexColumnCommand.class);
      return targetLookup.resolveByIndexId(command.indexId(), null);
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveRemoveIndexColumn(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      RemoveIndexColumnCommand command = requirePayload(payload, RemoveIndexColumnCommand.class);
      return targetLookup.resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeIndexColumnPosition(Object payload) {
    if (payload instanceof ChangeIndexColumnPositionInverse inverse) {
      return targetLookup.resolveByIndexColumnId(inverse.indexColumnId(), inverse.indexColumnId());
    }
    ChangeIndexColumnPositionCommand command = requirePayload(payload, ChangeIndexColumnPositionCommand.class);
    return targetLookup.resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeIndexColumnSortDirection(Object payload) {
    ChangeIndexColumnSortDirectionCommand command = requirePayload(
        payload, ChangeIndexColumnSortDirectionCommand.class);
    return targetLookup.resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
  }

}
