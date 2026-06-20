package com.schemafy.core.erd.column.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.domain.validator.ColumnValidator;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse.FkColumnTypeRevert;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByColumnIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = COLUMN, id = "columnId"))
public class ChangeColumnTypeService implements ChangeColumnTypeUseCase {

  private final ChangeColumnTypePort changeColumnTypePort;
  private final TransactionalOperator transactionalOperator;
  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetRelationshipColumnsByColumnIdPort getRelationshipColumnsByColumnIdPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeColumnType(ChangeColumnTypeCommand command) {
    ColumnTypeArguments typeArguments = ColumnTypeArguments.from(
        command.length(),
        command.precision(),
        command.scale(),
        command.values());
    Set<String> affectedTableIds = new HashSet<>();
    Set<String> capturedFkColumnIds = new HashSet<>();
    List<FkColumnTypeRevert> fkRevertList = new ArrayList<>();

    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
        .flatMap(column -> {
          affectedTableIds.add(column.tableId());
          return resolveDirectChange(column, command.dataType(), typeArguments)
              .flatMap(change -> {
                if (!change.hasDirectChange()) {
                  return Mono.just(MutationResult.<Void>of(null, affectedTableIds));
                }
                return validateCrossColumnRules(column, change)
                    .then(rejectIfForeignKeyColumn(command.columnId()))
                    .then(Mono.defer(() -> erdMutationCoordinator.coordinate(
                        ErdOperationType.CHANGE_COLUMN_TYPE,
                        command,
                        () -> applyChange(
                            column,
                            change,
                            affectedTableIds,
                            fkRevertList,
                            capturedFkColumnIds)
                            .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)
                                .withInverse(new ChangeColumnTypeInverse(
                                    column.id(),
                                    column.dataType(),
                                    column.typeArguments(),
                                    fkRevertList)))))));
              });
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> applyChange(
      Column column,
      DirectColumnTypeChange change,
      Set<String> affectedTableIds,
      List<FkColumnTypeRevert> fkRevertList,
      Set<String> capturedFkColumnIds) {
    String normalizedDataType = change.dataType();
    ColumnTypeArguments typeArguments = change.typeArguments();
    ResolvedColumnMeta targetMeta = change.targetMeta();

    return changeColumnTypePort.changeColumnType(column.id(), normalizedDataType, typeArguments)
        .then(applyDerivedMetaIfNeeded(column, targetMeta))
        .then(cascadeTypeToFkColumns(
            column,
            normalizedDataType,
            typeArguments,
            targetMeta,
            new HashSet<>(),
            affectedTableIds,
            fkRevertList,
            capturedFkColumnIds));
  }

  private Mono<DirectColumnTypeChange> resolveDirectChange(
      Column column,
      String dataType,
      ColumnTypeArguments typeArguments) {
    String normalizedDataType = ColumnValidator.normalizeDataType(dataType);
    ColumnValidator.validateDataType(normalizedDataType);
    ColumnValidator.validateTypeArguments(normalizedDataType, typeArguments);
    ColumnValidator.validateAutoIncrement(
        normalizedDataType,
        column.autoIncrement(),
        null,
        column.id());

    return resolveTargetMeta(column, normalizedDataType)
        .flatMap(targetMeta -> {
          ColumnValidator.validateCharsetAndCollation(
              normalizedDataType,
              targetMeta.charset(),
              targetMeta.collation());
          DirectColumnTypeChange directChange = new DirectColumnTypeChange(
              normalizedDataType,
              typeArguments,
              targetMeta,
              !Objects.equals(column.dataType(), normalizedDataType)
                  || !Objects.equals(column.typeArguments(), typeArguments)
                  || !Objects.equals(column.charset(), targetMeta.charset())
                  || !Objects.equals(column.collation(), targetMeta.collation()));
          return Mono.just(directChange);
        });
  }

  private Mono<Void> validateCrossColumnRules(Column column, DirectColumnTypeChange change) {
    return getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
        .defaultIfEmpty(List.of())
        .doOnNext(columns -> ColumnValidator.validateAutoIncrement(
            change.dataType(),
            column.autoIncrement(),
            columns,
            column.id()))
        .then();
  }

  private Mono<Void> rejectIfForeignKeyColumn(String columnId) {
    return getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(columnId)
        .defaultIfEmpty(List.of())
        .flatMap(relationshipColumns -> {
          boolean isFk = relationshipColumns.stream()
              .anyMatch(rc -> rc.fkColumnId().equals(columnId));
          if (isFk) {
            return Mono.error(new DomainException(ColumnErrorCode.FK_PROTECTED,
                "Foreign key column type cannot be changed directly"));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> cascadeTypeToFkColumns(
      Column pkColumn,
      String dataType,
      ColumnTypeArguments typeArguments,
      ResolvedColumnMeta targetMeta,
      Set<String> visited,
      Set<String> affectedTableIds,
      List<FkColumnTypeRevert> fkRevertList,
      Set<String> capturedFkColumnIds) {
    if (!visited.add(pkColumn.id())) {
      return Mono.empty();
    }
    return getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id())
        .defaultIfEmpty(List.of())
        .flatMap(constraintColumns -> Flux.fromIterable(constraintColumns)
            .concatMap(cc -> getConstraintByIdPort.findConstraintById(cc.constraintId()))
            .filter(constraint -> constraint.kind() == ConstraintKind.PRIMARY_KEY)
            .next()
            .flatMap(pk -> propagateTypeToFkColumns(
                pkColumn,
                dataType,
                typeArguments,
                targetMeta,
                visited,
                affectedTableIds,
                fkRevertList,
                capturedFkColumnIds)));
  }

  private Mono<Void> propagateTypeToFkColumns(
      Column pkColumn,
      String dataType,
      ColumnTypeArguments typeArguments,
      ResolvedColumnMeta targetMeta,
      Set<String> visited,
      Set<String> affectedTableIds,
      List<FkColumnTypeRevert> fkRevertList,
      Set<String> capturedFkColumnIds) {
    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> {
          affectedTableIds.add(relationship.fkTableId());
          return getRelationshipColumnsByRelationshipIdPort
              .findRelationshipColumnsByRelationshipId(relationship.id())
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable)
              .filter(rc -> rc.pkColumnId().equals(pkColumn.id()))
              .concatMap(rc -> getColumnByIdPort.findColumnById(rc.fkColumnId())
                  .switchIfEmpty(Mono.error(new DomainException(
                      ColumnErrorCode.NOT_FOUND,
                      "Column not found: " + rc.fkColumnId())))
                  .flatMap(fkColumn -> {
                    if (capturedFkColumnIds.add(fkColumn.id())) {
                      fkRevertList.add(new FkColumnTypeRevert(
                          fkColumn.id(),
                          fkColumn.dataType(),
                          fkColumn.typeArguments(),
                          fkColumn.charset(),
                          fkColumn.collation()));
                    }
                    Mono<Void> changeType = changeColumnTypePort.changeColumnType(
                        rc.fkColumnId(), dataType, typeArguments);
                    Mono<Void> syncCharsetCollation = applyDerivedMetaIfNeeded(fkColumn, targetMeta);
                    Column fkColumnForCascade = new Column(
                        fkColumn.id(),
                        fkColumn.tableId(),
                        fkColumn.name(),
                        dataType,
                        typeArguments,
                        fkColumn.seqNo(),
                        fkColumn.autoIncrement(),
                        targetMeta.charset(),
                        targetMeta.collation(),
                        fkColumn.comment());

                    return changeType
                        .then(syncCharsetCollation)
                        .then(cascadeTypeToFkColumns(
                            fkColumnForCascade,
                            dataType,
                            typeArguments,
                            targetMeta,
                            visited,
                            affectedTableIds,
                            fkRevertList,
                            capturedFkColumnIds));
                  }));
        })
        .then();
  }

  private Mono<ResolvedColumnMeta> resolveTargetMeta(Column column, String targetDataType) {
    if (!ColumnValidator.isTextType(targetDataType)) {
      return Mono.just(ResolvedColumnMeta.cleared());
    }
    if (ColumnValidator.isTextType(column.dataType())
        && hasText(column.charset())
        && hasText(column.collation())) {
      return Mono.just(new ResolvedColumnMeta(column.charset(), column.collation()));
    }

    return getTableByIdPort.findTableById(column.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
        .flatMap(table -> getSchemaByIdPort.findSchemaById(table.schemaId())
            .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found")))
            .map(schema -> resolveTextMeta(column, table, schema)));
  }

  private static ResolvedColumnMeta resolveTextMeta(Column column, Table table, Schema schema) {
    return new ResolvedColumnMeta(
        coalesce(column.charset(), table.charset(), schema.charset()),
        coalesce(column.collation(), table.collation(), schema.collation()));
  }

  private Mono<Void> applyDerivedMetaIfNeeded(Column column, ResolvedColumnMeta targetMeta) {
    if (Objects.equals(column.charset(), targetMeta.charset())
        && Objects.equals(column.collation(), targetMeta.collation())) {
      return Mono.empty();
    }

    return changeColumnMetaPort.changeColumnMeta(
        column.id(),
        null,
        toPortValue(targetMeta.charset()),
        toPortValue(targetMeta.collation()),
        null);
  }

  private static String coalesce(String... candidates) {
    for (String candidate : candidates) {
      if (candidate != null && !candidate.isBlank()) {
        return candidate.trim();
      }
    }
    return null;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String toPortValue(String value) {
    return value == null ? "" : value;
  }

  private record ResolvedColumnMeta(String charset, String collation) {

    static ResolvedColumnMeta cleared() {
      return new ResolvedColumnMeta(null, null);
    }

  }

  private record DirectColumnTypeChange(
      String dataType,
      ColumnTypeArguments typeArguments,
      ResolvedColumnMeta targetMeta,
      boolean hasDirectChange) {
  }

}
