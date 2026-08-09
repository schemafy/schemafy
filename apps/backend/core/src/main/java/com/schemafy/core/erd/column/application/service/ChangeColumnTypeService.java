package com.schemafy.core.erd.column.application.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.schemafy.core.erd.vendor.application.service.DatatypePolicyResolver;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeDefinition;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
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
  private final DatatypePolicyResolver datatypePolicyResolver;
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

    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
        .flatMap(column -> datatypePolicyResolver.resolve(COLUMN, column.id())
            .flatMap(datatypePolicy -> {
              affectedTableIds.add(column.tableId());
              return resolveDirectChange(datatypePolicy, column, command.dataType(), typeArguments)
                  .flatMap(change -> {
                    if (!change.hasDirectChange()) {
                      return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
                    }
                    return erdMutationCoordinator.coordinate(
                        ErdOperationType.CHANGE_COLUMN_TYPE,
                        command,
                        () -> getColumnByIdPort.findColumnById(command.columnId())
                            .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND,
                                "Column not found")))
                            .flatMap(lockedColumn -> {
                              affectedTableIds.add(lockedColumn.tableId());
                              return resolveDirectChange(
                                  datatypePolicy,
                                  lockedColumn,
                                  command.dataType(),
                                  typeArguments)
                                  .flatMap(lockedChange -> {
                                    if (!lockedChange.hasDirectChange()) {
                                      return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
                                    }
                                    return validateCrossColumnRules(lockedColumn, lockedChange)
                                        .then(rejectIfForeignKeyColumn(command.columnId()))
                                        .then(Mono.defer(() -> resolveFkTargets(
                                            datatypePolicy,
                                            lockedColumn,
                                            lockedChange)))
                                        .flatMap(fkTargets -> {
                                          affectedTableIds.addAll(fkTargets.stream()
                                              .map(target -> target.column().tableId())
                                              .toList());
                                          List<FkColumnTypeRevert> fkRevertList = fkTargets.stream()
                                              .map(FkColumnTypeTarget::toRevert)
                                              .toList();
                                          return applyChange(lockedColumn, lockedChange, fkTargets)
                                              .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                                                  .withInverse(new ChangeColumnTypeInverse(
                                                      lockedColumn.id(),
                                                      lockedColumn.dataType(),
                                                      lockedColumn.typeArguments(),
                                                      fkRevertList)));
                                        });
                                  });
                            }));
                  });
            }))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> applyChange(
      Column column,
      DirectColumnTypeChange change,
      List<FkColumnTypeTarget> fkTargets) {
    return changeColumnTypePort.changeColumnType(
        column.id(), change.dataType(), change.typeArguments())
        .then(applyDerivedMetaIfNeeded(column, change.targetMeta()))
        .thenMany(Flux.fromIterable(fkTargets)
            .concatMap(target -> changeColumnTypePort.changeColumnType(
                target.column().id(), change.dataType(), change.typeArguments())
                .then(applyDerivedMetaIfNeeded(target.column(), change.targetMeta()))))
        .then();
  }

  private Mono<DirectColumnTypeChange> resolveDirectChange(
      DatatypePolicy datatypePolicy,
      Column column,
      String dataType,
      ColumnTypeArguments typeArguments) {
    DatatypeDefinition datatype = DatatypePolicyColumnValidator.validate(
        datatypePolicy,
        dataType,
        typeArguments,
        column.autoIncrement(),
        null,
        null);

    return resolveTargetMeta(datatypePolicy, column, datatype)
        .flatMap(targetMeta -> {
          DatatypePolicyColumnValidator.validate(
              datatypePolicy,
              datatype.sqlType(),
              typeArguments,
              column.autoIncrement(),
              targetMeta.charset(),
              targetMeta.collation());
          DirectColumnTypeChange directChange = new DirectColumnTypeChange(
              datatype.sqlType(),
              typeArguments,
              targetMeta,
              !Objects.equals(column.dataType(), datatype.sqlType())
                  || !Objects.equals(column.typeArguments(), typeArguments)
                  || !Objects.equals(column.charset(), targetMeta.charset())
                  || !Objects.equals(column.collation(), targetMeta.collation()));
          return Mono.just(directChange);
        });
  }

  private Mono<Void> validateCrossColumnRules(Column column, DirectColumnTypeChange change) {
    return getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
        .defaultIfEmpty(List.of())
        .doOnNext(columns -> ColumnValidator.validateAutoIncrementUniqueness(
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

  private Mono<List<FkColumnTypeTarget>> resolveFkTargets(
      DatatypePolicy datatypePolicy,
      Column rootColumn,
      DirectColumnTypeChange change) {
    Map<String, FkColumnTypeTarget> targets = new LinkedHashMap<>();
    return collectFkTargets(
        datatypePolicy,
        rootColumn,
        change,
        new HashSet<>(),
        targets)
        .then(Mono.fromCallable(() -> List.copyOf(targets.values())));
  }

  private Mono<Void> collectFkTargets(
      DatatypePolicy datatypePolicy,
      Column pkColumn,
      DirectColumnTypeChange change,
      Set<String> visited,
      Map<String, FkColumnTypeTarget> targets) {
    if (!visited.add(pkColumn.id())) {
      return Mono.empty();
    }
    return getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id())
        .defaultIfEmpty(List.of())
        .flatMap(constraintColumns -> Flux.fromIterable(constraintColumns)
            .concatMap(cc -> getConstraintByIdPort.findConstraintById(cc.constraintId()))
            .filter(constraint -> constraint.kind() == ConstraintKind.PRIMARY_KEY)
            .next()
            .flatMap(pk -> collectRelatedFkTargets(
                datatypePolicy,
                pkColumn,
                change,
                visited,
                targets)));
  }

  private Mono<Void> collectRelatedFkTargets(
      DatatypePolicy datatypePolicy,
      Column pkColumn,
      DirectColumnTypeChange change,
      Set<String> visited,
      Map<String, FkColumnTypeTarget> targets) {
    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationship.id())
            .defaultIfEmpty(List.of())
            .flatMapMany(Flux::fromIterable)
            .filter(rc -> rc.pkColumnId().equals(pkColumn.id()))
            .concatMap(rc -> getColumnByIdPort.findColumnById(rc.fkColumnId())
                .switchIfEmpty(Mono.error(new DomainException(
                    ColumnErrorCode.NOT_FOUND,
                    "Column not found: " + rc.fkColumnId())))
                .flatMap(fkColumn -> {
                  if (visited.contains(fkColumn.id())) {
                    return Mono.empty();
                  }
                  DatatypePolicyColumnValidator.validate(
                      datatypePolicy,
                      change.dataType(),
                      change.typeArguments(),
                      fkColumn.autoIncrement(),
                      change.targetMeta().charset(),
                      change.targetMeta().collation());
                  Column fkColumnForCascade = new Column(
                      fkColumn.id(),
                      fkColumn.tableId(),
                      fkColumn.name(),
                      change.dataType(),
                      change.typeArguments(),
                      fkColumn.seqNo(),
                      fkColumn.autoIncrement(),
                      change.targetMeta().charset(),
                      change.targetMeta().collation(),
                      fkColumn.comment());
                  if (targets.putIfAbsent(
                      fkColumn.id(),
                      new FkColumnTypeTarget(fkColumn)) != null) {
                    return Mono.empty();
                  }
                  return collectFkTargets(
                      datatypePolicy,
                      fkColumnForCascade,
                      change,
                      visited,
                      targets);
                })))
        .then();
  }

  private Mono<ResolvedColumnMeta> resolveTargetMeta(
      DatatypePolicy datatypePolicy,
      Column column,
      DatatypeDefinition targetDatatype) {
    if (!targetDatatype.properties().charsetCollationAllowed()) {
      return Mono.just(ResolvedColumnMeta.cleared());
    }
    if (datatypePolicy.find(column.dataType())
        .map(current -> current.properties().charsetCollationAllowed())
        .orElse(false)
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

  private record FkColumnTypeTarget(Column column) {

    FkColumnTypeRevert toRevert() {
      return new FkColumnTypeRevert(
          column.id(),
          column.dataType(),
          column.typeArguments(),
          column.charset(),
          column.collation());
    }

  }

}
