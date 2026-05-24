package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.requirePayload;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.resolveStructuralOr;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.unsupportedTargetOperation;

@Component
@RequiredArgsConstructor
class TableMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolve(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CREATE_TABLE -> resolveCreateTable(payload);
    case CHANGE_TABLE_NAME -> resolveChangeTableName(payload);
    case CHANGE_TABLE_META -> resolveChangeTableMeta(payload);
    case CHANGE_TABLE_EXTRA -> resolveChangeTableExtra(payload);
    case DELETE_TABLE -> resolveDeleteTable(payload);
    default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveCreateTable(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      CreateTableCommand command = requirePayload(payload, CreateTableCommand.class);
      return targetLookup.resolveSchemaContext(command.schemaId());
    });
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeTableName(Object payload) {
    if (payload instanceof ChangeTableNameInverse inverse) {
      return targetLookup.resolveByTableId(inverse.tableId(), inverse.tableId());
    }
    ChangeTableNameCommand command = requirePayload(payload, ChangeTableNameCommand.class);
    return targetLookup.resolveByTableId(command.tableId(), command.tableId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeTableMeta(Object payload) {
    ChangeTableMetaCommand command = requirePayload(payload, ChangeTableMetaCommand.class);
    return targetLookup.resolveByTableId(command.tableId(), command.tableId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeTableExtra(Object payload) {
    ChangeTableExtraCommand command = requirePayload(payload, ChangeTableExtraCommand.class);
    return targetLookup.resolveByTableId(command.tableId(), command.tableId());
  }

  private Mono<ResolvedErdMutationTarget> resolveDeleteTable(Object payload) {
    return resolveStructuralOr(payload, targetLookup, () -> {
      DeleteTableCommand command = requirePayload(payload, DeleteTableCommand.class);
      return targetLookup.resolveByTableId(command.tableId(), command.tableId());
    });
  }

}
