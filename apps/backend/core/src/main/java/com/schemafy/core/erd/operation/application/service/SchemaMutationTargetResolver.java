package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.requirePayload;
import static com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolutionSupport.unsupportedTargetOperation;

@Component
@RequiredArgsConstructor
class SchemaMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolve(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_SCHEMA -> resolveCreateSchema(payload);
      case CHANGE_SCHEMA_NAME -> resolveChangeSchemaName(payload);
      case DELETE_SCHEMA -> resolveDeleteSchema(payload);
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveCreateSchema(Object payload) {
    CreateSchemaCommand command = requirePayload(payload, CreateSchemaCommand.class);
    return Mono.just(new ResolvedErdMutationTarget(command.projectId(), null, null));
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeSchemaName(Object payload) {
    ChangeSchemaNameCommand command = requirePayload(payload, ChangeSchemaNameCommand.class);
    return targetLookup.resolveBySchemaId(command.schemaId(), command.schemaId());
  }

  private Mono<ResolvedErdMutationTarget> resolveDeleteSchema(Object payload) {
    DeleteSchemaCommand command = requirePayload(payload, DeleteSchemaCommand.class);
    return targetLookup.resolveBySchemaId(command.schemaId(), command.schemaId());
  }

}
