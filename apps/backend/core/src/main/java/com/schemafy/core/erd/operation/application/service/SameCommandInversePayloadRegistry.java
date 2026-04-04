package com.schemafy.core.erd.operation.application.service;

import java.util.EnumMap;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Mono;

@Component
class SameCommandInversePayloadRegistry {

  private final EnumMap<ErdOperationType, SameCommandInversePayloadSpec<?>> specs =
      new EnumMap<>(ErdOperationType.class);

  SameCommandInversePayloadRegistry(
      GetTableByIdPort getTableByIdPort,
      GetColumnByIdPort getColumnByIdPort,
      GetRelationshipByIdPort getRelationshipByIdPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetIndexByIdPort getIndexByIdPort) {
    register(
        ErdOperationType.CHANGE_TABLE_NAME,
        ChangeTableNameCommand.class,
        command -> resolveTableNameInverse(command, getTableByIdPort));
    register(
        ErdOperationType.CHANGE_COLUMN_NAME,
        ChangeColumnNameCommand.class,
        command -> resolveColumnNameInverse(command, getColumnByIdPort));
    register(
        ErdOperationType.CHANGE_COLUMN_TYPE,
        ChangeColumnTypeCommand.class,
        command -> resolveColumnTypeInverse(command, getColumnByIdPort));
    register(
        ErdOperationType.CHANGE_RELATIONSHIP_NAME,
        ChangeRelationshipNameCommand.class,
        command -> resolveRelationshipNameInverse(command, getRelationshipByIdPort));
    register(
        ErdOperationType.CHANGE_RELATIONSHIP_KIND,
        ChangeRelationshipKindCommand.class,
        command -> resolveRelationshipKindInverse(command, getRelationshipByIdPort));
    register(
        ErdOperationType.CHANGE_RELATIONSHIP_CARDINALITY,
        ChangeRelationshipCardinalityCommand.class,
        command -> resolveRelationshipCardinalityInverse(command, getRelationshipByIdPort));
    register(
        ErdOperationType.CHANGE_CONSTRAINT_NAME,
        ChangeConstraintNameCommand.class,
        command -> resolveConstraintNameInverse(command, getConstraintByIdPort));
    register(
        ErdOperationType.CHANGE_INDEX_NAME,
        ChangeIndexNameCommand.class,
        command -> resolveIndexNameInverse(command, getIndexByIdPort));
    register(
        ErdOperationType.CHANGE_INDEX_TYPE,
        ChangeIndexTypeCommand.class,
        command -> resolveIndexTypeInverse(command, getIndexByIdPort));
  }

  Mono<Object> resolve(ErdOperationType opType, Object payload) {
    return Mono.defer(() -> {
      SameCommandInversePayloadSpec<?> spec = specs.get(opType);
      if (spec == null) {
        return Mono.empty();
      }
      return spec.resolve(payload);
    });
  }

  boolean supports(ErdOperationType opType) {
    return opType != null && specs.containsKey(opType);
  }

  Set<ErdOperationType> supportedOperationTypes() {
    return Set.copyOf(specs.keySet());
  }

  private <T> void register(
      ErdOperationType opType,
      Class<T> payloadType,
      Function<T, Mono<Object>> inversePayloadResolver) {
    specs.put(opType, new SameCommandInversePayloadSpec<>(payloadType, inversePayloadResolver));
  }

  private static Mono<Object> resolveTableNameInverse(
      ChangeTableNameCommand command,
      GetTableByIdPort getTableByIdPort) {
    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, tableNotFound(command.tableId()))))
        .map(table -> (Object) new ChangeTableNameCommand(table.id(), table.name()));
  }

  private static Mono<Object> resolveColumnNameInverse(
      ChangeColumnNameCommand command,
      GetColumnByIdPort getColumnByIdPort) {
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, columnNotFound(command.columnId()))))
        .map(column -> (Object) new ChangeColumnNameCommand(column.id(), column.name()));
  }

  private static Mono<Object> resolveColumnTypeInverse(
      ChangeColumnTypeCommand command,
      GetColumnByIdPort getColumnByIdPort) {
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, columnNotFound(command.columnId()))))
        .map(column -> {
          ColumnTypeArguments typeArguments = column.typeArguments();
          return (Object) new ChangeColumnTypeCommand(
              column.id(),
              column.dataType(),
              typeArguments == null ? null : typeArguments.length(),
              typeArguments == null ? null : typeArguments.precision(),
              typeArguments == null ? null : typeArguments.scale(),
              typeArguments == null ? null : typeArguments.values());
        });
  }

  private static Mono<Object> resolveRelationshipNameInverse(
      ChangeRelationshipNameCommand command,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            relationshipNotFound(command.relationshipId()))))
        .map(relationship -> (Object) new ChangeRelationshipNameCommand(relationship.id(), relationship.name()));
  }

  private static Mono<Object> resolveRelationshipKindInverse(
      ChangeRelationshipKindCommand command,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            relationshipNotFound(command.relationshipId()))))
        .map(relationship -> (Object) new ChangeRelationshipKindCommand(relationship.id(), relationship.kind()));
  }

  private static Mono<Object> resolveRelationshipCardinalityInverse(
      ChangeRelationshipCardinalityCommand command,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            relationshipNotFound(command.relationshipId()))))
        .map(relationship -> (Object) new ChangeRelationshipCardinalityCommand(
            relationship.id(),
            relationship.cardinality()));
  }

  private static Mono<Object> resolveConstraintNameInverse(
      ChangeConstraintNameCommand command,
      GetConstraintByIdPort getConstraintByIdPort) {
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            constraintNotFound(command.constraintId()))))
        .map(constraint -> (Object) new ChangeConstraintNameCommand(constraint.id(), constraint.name()));
  }

  private static Mono<Object> resolveIndexNameInverse(
      ChangeIndexNameCommand command,
      GetIndexByIdPort getIndexByIdPort) {
    return getIndexByIdPort.findIndexById(command.indexId())
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, indexNotFound(command.indexId()))))
        .map(index -> (Object) new ChangeIndexNameCommand(index.id(), index.name()));
  }

  private static Mono<Object> resolveIndexTypeInverse(
      ChangeIndexTypeCommand command,
      GetIndexByIdPort getIndexByIdPort) {
    return getIndexByIdPort.findIndexById(command.indexId())
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, indexNotFound(command.indexId()))))
        .map(index -> (Object) new ChangeIndexTypeCommand(index.id(), index.type()));
  }

  private static String tableNotFound(String tableId) {
    return "Table not found: " + tableId;
  }

  private static String columnNotFound(String columnId) {
    return "Column not found: " + columnId;
  }

  private static String relationshipNotFound(String relationshipId) {
    return "Relationship not found: " + relationshipId;
  }

  private static String constraintNotFound(String constraintId) {
    return "Constraint not found: " + constraintId;
  }

  private static String indexNotFound(String indexId) {
    return "Index not found: " + indexId;
  }

  private record SameCommandInversePayloadSpec<T>(
      Class<T> payloadType,
      Function<T, Mono<Object>> inversePayloadResolver) implements TypedPayloadSpec<T> {

    private Mono<Object> resolve(Object payload) {
      return inversePayloadResolver.apply(requirePayload(payload, "Unsupported inverse payload type: "));
    }
  }

}
