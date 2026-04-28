package com.schemafy.core.erd.operation.application.service;

import java.util.EnumMap;
import java.util.List;
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
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.core.erd.relationship.domain.AutoRelationshipNaming;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
class SameCommandInversePayloadRegistry {

  private final EnumMap<ErdOperationType, SameCommandInversePayloadSpec<?>> specs = new EnumMap<>(
      ErdOperationType.class);

  SameCommandInversePayloadRegistry(
      GetTableByIdPort getTableByIdPort,
      GetConstraintsByTableIdPort getConstraintsByTableIdPort,
      GetRelationshipsByTableIdPort getRelationshipsByTableIdPort,
      GetColumnByIdPort getColumnByIdPort,
      GetRelationshipByIdPort getRelationshipByIdPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetIndexByIdPort getIndexByIdPort) {
    register(
        ErdOperationType.CHANGE_TABLE_NAME,
        ChangeTableNameCommand.class,
        command -> resolveTableNameInverse(
            command,
            getTableByIdPort,
            getConstraintsByTableIdPort,
            getRelationshipsByTableIdPort));
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

  private <T> void register(
      ErdOperationType opType,
      Class<T> payloadType,
      Function<T, Mono<Object>> inversePayloadResolver) {
    specs.put(opType, new SameCommandInversePayloadSpec<>(payloadType, inversePayloadResolver));
  }

  private static Mono<Object> resolveTableNameInverse(
      ChangeTableNameCommand command,
      GetTableByIdPort getTableByIdPort,
      GetConstraintsByTableIdPort getConstraintsByTableIdPort,
      GetRelationshipsByTableIdPort getRelationshipsByTableIdPort) {
    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, tableNotFound(command.tableId()))))
        .flatMap(table -> Mono.zip(
            resolveConstraintNameRestores(table, getConstraintsByTableIdPort),
            resolveRelationshipNameRestores(table, getTableByIdPort, getRelationshipsByTableIdPort))
            .map(tuple -> (Object) new ChangeTableNameReplayPayload(
                table.id(),
                table.name(),
                tuple.getT1(),
                tuple.getT2())));
  }

  private static Mono<List<ChangeTableNameReplayPayload.NameRestore>> resolveConstraintNameRestores(
      Table table,
      GetConstraintsByTableIdPort getConstraintsByTableIdPort) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(table.id())
        .defaultIfEmpty(List.of())
        .map(constraints -> constraints.stream()
            .filter(constraint -> constraint.kind() == ConstraintKind.PRIMARY_KEY)
            .findFirst()
            .map(SameCommandInversePayloadRegistry::toNameRestore)
            .stream()
            .toList());
  }

  private static Mono<List<ChangeTableNameReplayPayload.NameRestore>> resolveRelationshipNameRestores(
      Table table,
      GetTableByIdPort getTableByIdPort,
      GetRelationshipsByTableIdPort getRelationshipsByTableIdPort) {
    return getRelationshipsByTableIdPort.findRelationshipsByTableId(table.id())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> resolveRelationshipNameRestore(relationship, table, getTableByIdPort))
        .collectList();
  }

  private static Mono<ChangeTableNameReplayPayload.NameRestore> resolveRelationshipNameRestore(
      Relationship relationship,
      Table renamedTable,
      GetTableByIdPort getTableByIdPort) {
    boolean isFkRenamed = relationship.fkTableId().equals(renamedTable.id());
    boolean isPkRenamed = relationship.pkTableId().equals(renamedTable.id());
    if (!isFkRenamed && !isPkRenamed) {
      return Mono.empty();
    }
    if (isFkRenamed && isPkRenamed) {
      return Mono.justOrEmpty(toAutoRelationshipNameRestore(
          relationship,
          renamedTable.name(),
          renamedTable.name()));
    }

    String otherTableId = isFkRenamed ? relationship.pkTableId() : relationship.fkTableId();
    return getTableByIdPort.findTableById(otherTableId)
        .flatMap(otherTable -> Mono.justOrEmpty(toAutoRelationshipNameRestore(
            relationship,
            isFkRenamed ? renamedTable.name() : otherTable.name(),
            isPkRenamed ? renamedTable.name() : otherTable.name())));
  }

  private static ChangeTableNameReplayPayload.NameRestore toAutoRelationshipNameRestore(
      Relationship relationship,
      String oldFkName,
      String oldPkName) {
    String oldBaseName = AutoRelationshipNaming.buildBaseName(oldFkName, oldPkName);
    if (!AutoRelationshipNaming.matches(relationship.name(), oldBaseName)) {
      return null;
    }
    return toNameRestore(relationship);
  }

  private static ChangeTableNameReplayPayload.NameRestore toNameRestore(Constraint constraint) {
    return new ChangeTableNameReplayPayload.NameRestore(constraint.id(), constraint.name());
  }

  private static ChangeTableNameReplayPayload.NameRestore toNameRestore(Relationship relationship) {
    return new ChangeTableNameReplayPayload.NameRestore(relationship.id(), relationship.name());
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
