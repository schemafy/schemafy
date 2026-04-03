package com.schemafy.core.erd.operation.application.service;

import java.util.Set;
import java.util.Objects;
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
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class SameCommandUndoRedoStrategy implements UndoRedoErdOperationStrategy {

  private static final Set<ErdOperationType> SUPPORTED_OPERATION_TYPES = Set.of(
      ErdOperationType.CHANGE_TABLE_NAME,
      ErdOperationType.CHANGE_COLUMN_NAME,
      ErdOperationType.CHANGE_COLUMN_TYPE,
      ErdOperationType.CHANGE_RELATIONSHIP_NAME,
      ErdOperationType.CHANGE_RELATIONSHIP_KIND,
      ErdOperationType.CHANGE_RELATIONSHIP_CARDINALITY,
      ErdOperationType.CHANGE_CONSTRAINT_NAME,
      ErdOperationType.CHANGE_INDEX_NAME,
      ErdOperationType.CHANGE_INDEX_TYPE);

  private final ChangeTableNameUseCase changeTableNameUseCase;
  private final ChangeColumnNameUseCase changeColumnNameUseCase;
  private final ChangeColumnTypeUseCase changeColumnTypeUseCase;
  private final ChangeRelationshipNameUseCase changeRelationshipNameUseCase;
  private final ChangeRelationshipKindUseCase changeRelationshipKindUseCase;
  private final ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;
  private final ChangeConstraintNameUseCase changeConstraintNameUseCase;
  private final ChangeIndexNameUseCase changeIndexNameUseCase;
  private final ChangeIndexTypeUseCase changeIndexTypeUseCase;
  private final JsonCodec jsonCodec;

  @Override
  public boolean supports(ErdOperationType opType) {
    return SUPPORTED_OPERATION_TYPES.contains(opType);
  }

  @Override
  public Mono<MutationResult<Void>> undo(ErdOperationLog operationLog) {
    Objects.requireNonNull(operationLog, "operationLog");
    return execute(
        operationLog,
        operationLog.inversePayloadJson(),
        ErdOperationDerivationKind.UNDO,
        "Undo payload is missing for operation: " + operationLog.opId());
  }

  @Override
  public Mono<MutationResult<Void>> redo(ErdOperationLog operationLog) {
    Objects.requireNonNull(operationLog, "operationLog");
    return execute(
        operationLog,
        operationLog.payloadJson(),
        ErdOperationDerivationKind.REDO,
        "Redo payload is missing for operation: " + operationLog.opId());
  }

  private Mono<MutationResult<Void>> execute(
      ErdOperationLog operationLog,
      String payloadJson,
      ErdOperationDerivationKind derivationKind,
      String missingPayloadMessage) {
    if (payloadJson == null || payloadJson.isBlank()) {
      return Mono.error(new IllegalStateException(missingPayloadMessage));
    }

    return switch (operationLog.opType()) {
    case CHANGE_TABLE_NAME -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeTableNameCommand.class,
        changeTableNameUseCase::changeTableName);
    case CHANGE_COLUMN_NAME -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeColumnNameCommand.class,
        changeColumnNameUseCase::changeColumnName);
    case CHANGE_COLUMN_TYPE -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeColumnTypeCommand.class,
        changeColumnTypeUseCase::changeColumnType);
    case CHANGE_RELATIONSHIP_NAME -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeRelationshipNameCommand.class,
        changeRelationshipNameUseCase::changeRelationshipName);
    case CHANGE_RELATIONSHIP_KIND -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeRelationshipKindCommand.class,
        changeRelationshipKindUseCase::changeRelationshipKind);
    case CHANGE_RELATIONSHIP_CARDINALITY -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeRelationshipCardinalityCommand.class,
        changeRelationshipCardinalityUseCase::changeRelationshipCardinality);
    case CHANGE_CONSTRAINT_NAME -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeConstraintNameCommand.class,
        changeConstraintNameUseCase::changeConstraintName);
    case CHANGE_INDEX_NAME -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeIndexNameCommand.class,
        changeIndexNameUseCase::changeIndexName);
    case CHANGE_INDEX_TYPE -> execute(
        operationLog,
        payloadJson,
        derivationKind,
        ChangeIndexTypeCommand.class,
        changeIndexTypeUseCase::changeIndexType);
    default -> Mono.error(new IllegalArgumentException(
        "Unsupported same-command undo/redo operation: " + operationLog.opType()));
    };
  }

  private <T> Mono<MutationResult<Void>> execute(
      ErdOperationLog operationLog,
      String payloadJson,
      ErdOperationDerivationKind derivationKind,
      Class<T> payloadType,
      Function<T, Mono<MutationResult<Void>>> invoker) {
    return invoker.apply(jsonCodec.parsePersisted(payloadJson, payloadType))
        .contextWrite(ErdOperationContexts.withDerivedFromOpId(operationLog.opId()))
        .contextWrite(ErdOperationContexts.withDerivationKind(derivationKind));
  }

}
