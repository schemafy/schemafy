package com.schemafy.core.erd.operation.application.service;

import java.util.EnumMap;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
class SameCommandReplayRegistry {

  private final EnumMap<ErdOperationType, SameCommandReplaySpec<?>> specs = new EnumMap<>(ErdOperationType.class);
  private final JsonCodec jsonCodec;

  SameCommandReplayRegistry(
      JsonCodec jsonCodec,
      ChangeTableNameUseCase changeTableNameUseCase,
      ChangeConstraintNamePort changeConstraintNamePort,
      ChangeRelationshipNamePort changeRelationshipNamePort,
      ChangeColumnNameUseCase changeColumnNameUseCase,
      ChangeColumnTypeUseCase changeColumnTypeUseCase,
      ChangeRelationshipNameUseCase changeRelationshipNameUseCase,
      ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase,
      ChangeConstraintNameUseCase changeConstraintNameUseCase,
      ChangeIndexNameUseCase changeIndexNameUseCase,
      ChangeIndexTypeUseCase changeIndexTypeUseCase) {
    this.jsonCodec = jsonCodec;
    register(
        ErdOperationType.CHANGE_TABLE_NAME,
        ChangeTableNameReplayPayload.class,
        payload -> replayTableName(
            payload,
            changeTableNameUseCase,
            changeConstraintNamePort,
            changeRelationshipNamePort));
    register(ErdOperationType.CHANGE_COLUMN_NAME, ChangeColumnNameCommand.class,
        changeColumnNameUseCase::changeColumnName);
    register(ErdOperationType.CHANGE_COLUMN_TYPE, ChangeColumnTypeCommand.class,
        changeColumnTypeUseCase::changeColumnType);
    register(
        ErdOperationType.CHANGE_RELATIONSHIP_NAME,
        ChangeRelationshipNameCommand.class,
        changeRelationshipNameUseCase::changeRelationshipName);
    register(
        ErdOperationType.CHANGE_RELATIONSHIP_CARDINALITY,
        ChangeRelationshipCardinalityCommand.class,
        changeRelationshipCardinalityUseCase::changeRelationshipCardinality);
    register(
        ErdOperationType.CHANGE_CONSTRAINT_NAME,
        ChangeConstraintNameCommand.class,
        changeConstraintNameUseCase::changeConstraintName);
    register(ErdOperationType.CHANGE_INDEX_NAME, ChangeIndexNameCommand.class, changeIndexNameUseCase::changeIndexName);
    register(ErdOperationType.CHANGE_INDEX_TYPE, ChangeIndexTypeCommand.class, changeIndexTypeUseCase::changeIndexType);
  }

  Mono<MutationResult<Void>> executePersisted(ErdOperationType opType, String payloadJson) {
    return Mono.defer(() -> {
      SameCommandReplaySpec<?> spec = requireSpec(opType);
      Object payload = parsePersistedPayload(payloadJson, spec);
      return spec.execute(payload);
    });
  }

  boolean supports(ErdOperationType opType) {
    return opType != null && specs.containsKey(opType);
  }

  private Object parsePersistedPayload(String payloadJson, SameCommandReplaySpec<?> spec) {
    return jsonCodec.parsePersisted(payloadJson, spec.payloadType());
  }

  private <T> void register(
      ErdOperationType opType,
      Class<T> payloadType,
      Function<T, Mono<MutationResult<Void>>> invoker) {
    specs.put(opType, new SameCommandReplaySpec<>(payloadType, invoker));
  }

  private static Mono<MutationResult<Void>> replayTableName(
      ChangeTableNameReplayPayload payload,
      ChangeTableNameUseCase changeTableNameUseCase,
      ChangeConstraintNamePort changeConstraintNamePort,
      ChangeRelationshipNamePort changeRelationshipNamePort) {
    return changeTableNameUseCase.changeTableName(payload.toCommand())
        .flatMap(result -> restoreTableNameSideEffectNames(
            payload,
            changeConstraintNamePort,
            changeRelationshipNamePort)
            .thenReturn(result));
  }

  private static Mono<Void> restoreTableNameSideEffectNames(
      ChangeTableNameReplayPayload payload,
      ChangeConstraintNamePort changeConstraintNamePort,
      ChangeRelationshipNamePort changeRelationshipNamePort) {
    return Flux.fromIterable(payload.constraintNameRestores())
        .concatMap(restore -> changeConstraintNamePort.changeConstraintName(restore.id(), restore.name()))
        .thenMany(Flux.fromIterable(payload.relationshipNameRestores())
            .concatMap(restore -> changeRelationshipNamePort.changeRelationshipName(restore.id(), restore.name())))
        .then();
  }

  private SameCommandReplaySpec<?> requireSpec(ErdOperationType opType) {
    SameCommandReplaySpec<?> spec = specs.get(opType);
    if (spec != null) {
      return spec;
    }
    throw new IllegalArgumentException("Unsupported same-command undo/redo operation: " + opType);
  }

  private record SameCommandReplaySpec<T>(
      Class<T> payloadType,
      Function<T, Mono<MutationResult<Void>>> invoker) implements TypedPayloadSpec<T> {

    private Mono<MutationResult<Void>> execute(Object payload) {
      return invoker.apply(requirePayload(payload, "Unsupported replay payload type: "));
    }

  }

}
