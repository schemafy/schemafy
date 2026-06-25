package com.schemafy.core.erd.column.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.domain.validator.ColumnValidator;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByColumnIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
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
public class ChangeColumnMetaService implements ChangeColumnMetaUseCase {

  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final TransactionalOperator transactionalOperator;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetRelationshipColumnsByColumnIdPort getRelationshipColumnsByColumnIdPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeColumnMeta(ChangeColumnMetaCommand command) {
    Set<String> affectedTableIds = ConcurrentHashMap.newKeySet();
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
        .flatMap(column -> {
          affectedTableIds.add(column.tableId());
          return resolveDirectChange(column, command)
              .flatMap(change -> {
                if (!change.hasDirectChange()) {
                  return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
                }
                return erdMutationCoordinator.coordinate(
                    ErdOperationType.CHANGE_COLUMN_META,
                    command,
                    () -> getColumnByIdPort.findColumnById(command.columnId())
                        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
                        .flatMap(lockedColumn -> {
                          affectedTableIds.add(lockedColumn.tableId());
                          return resolveDirectChange(lockedColumn, command)
                              .flatMap(lockedChange -> {
                                if (!lockedChange.hasDirectChange()) {
                                  return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
                                }
                                return validateCrossColumnRules(lockedColumn, lockedChange)
                                    .then(rejectIfForeignKeyColumn(command.columnId()))
                                    .then(Mono.defer(() -> applyChange(
                                        lockedColumn,
                                        lockedChange,
                                        affectedTableIds)))
                                    .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
                              });
                        }));
              });
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> rejectIfForeignKeyColumn(String columnId) {
    return getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(columnId)
        .defaultIfEmpty(List.of())
        .flatMap(relationshipColumns -> {
          boolean isFk = relationshipColumns.stream()
              .anyMatch(rc -> rc.fkColumnId().equals(columnId));
          if (isFk) {
            return Mono.error(new DomainException(ColumnErrorCode.FK_PROTECTED,
                "Foreign key column metadata cannot be changed directly"));
          }
          return Mono.empty();
        });
  }

  private Mono<DirectColumnMetaChange> resolveDirectChange(
      Column column,
      ChangeColumnMetaCommand command) {

    boolean effectiveAutoIncrement = command.autoIncrement().orElse(column.autoIncrement());
    String effectiveCharset = command.charset().isPresent()
        ? normalizeOptional(command.charset().get())
        : column.charset();
    String effectiveCollation = command.collation().isPresent()
        ? normalizeOptional(command.collation().get())
        : column.collation();
    String effectiveComment = command.comment().isPresent()
        ? normalizeOptional(command.comment().get())
        : column.comment();

    String normalizedDataType = ColumnValidator.normalizeDataType(column.dataType());
    ColumnValidator.validateAutoIncrement(
        normalizedDataType,
        effectiveAutoIncrement,
        null,
        column.id());
    ColumnValidator.validateCharsetAndCollation(normalizedDataType, effectiveCharset, effectiveCollation);

    DirectColumnMetaChange directChange = new DirectColumnMetaChange(
        command.autoIncrement().isPresent() ? effectiveAutoIncrement : null,
        command.charset().isPresent()
            ? Objects.toString(effectiveCharset, "")
            : null,
        command.collation().isPresent()
            ? Objects.toString(effectiveCollation, "")
            : null,
        command.comment().isPresent()
            ? Objects.toString(effectiveComment, "")
            : null,
        effectiveAutoIncrement,
        (command.autoIncrement().isPresent() && column.autoIncrement() != effectiveAutoIncrement)
            || (command.charset().isPresent() && !Objects.equals(column.charset(), effectiveCharset))
            || (command.collation().isPresent() && !Objects.equals(column.collation(), effectiveCollation))
            || (command.comment().isPresent() && !Objects.equals(column.comment(), effectiveComment)));

    return Mono.just(directChange);
  }

  private Mono<Void> validateCrossColumnRules(Column column, DirectColumnMetaChange change) {
    return getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
        .defaultIfEmpty(List.of())
        .doOnNext(columns -> ColumnValidator.validateAutoIncrement(
            ColumnValidator.normalizeDataType(column.dataType()),
            change.effectiveAutoIncrement(),
            columns,
            column.id()))
        .then();
  }

  private Mono<Void> applyChange(
      Column column,
      DirectColumnMetaChange change,
      Set<String> affectedTableIds) {
    return changeColumnMetaPort.changeColumnMeta(
        column.id(),
        change.portAutoIncrement(),
        change.portCharset(),
        change.portCollation(),
        change.portComment())
        .then(cascadeCharsetCollationToFkColumns(
            column,
            change.portCharset(),
            change.portCollation(),
            new HashSet<>(),
            affectedTableIds));
  }

  private Mono<Void> cascadeCharsetCollationToFkColumns(
      Column column,
      String charset,
      String collation,
      Set<String> visited,
      Set<String> affectedTableIds) {
    if (!visited.add(column.id())) {
      return Mono.empty();
    }
    return getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(column.id())
        .defaultIfEmpty(List.of())
        .flatMap(constraintColumns -> Flux.fromIterable(constraintColumns)
            .flatMap(cc -> getConstraintByIdPort.findConstraintById(cc.constraintId()))
            .filter(constraint -> constraint.kind() == ConstraintKind.PRIMARY_KEY)
            .next()
            .flatMap(pk -> propagateCharsetCollationToFkColumns(
                column, charset, collation, visited, affectedTableIds)));
  }

  private Mono<Void> propagateCharsetCollationToFkColumns(
      Column pkColumn,
      String charset,
      String collation,
      Set<String> visited,
      Set<String> affectedTableIds) {
    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .flatMap(relationship -> {
          affectedTableIds.add(relationship.fkTableId());
          return getRelationshipColumnsByRelationshipIdPort
              .findRelationshipColumnsByRelationshipId(relationship.id())
              .defaultIfEmpty(List.of())
              .flatMapMany(Flux::fromIterable)
              .filter(rc -> rc.pkColumnId().equals(pkColumn.id()))
              .flatMap(rc -> changeColumnMetaPort.changeColumnMeta(
                  rc.fkColumnId(), null, charset, collation, null)
                  .then(getColumnByIdPort.findColumnById(rc.fkColumnId())
                      .flatMap(fkCol -> cascadeCharsetCollationToFkColumns(
                          fkCol, charset, collation, visited, affectedTableIds))));
        })
        .then();
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private record DirectColumnMetaChange(
      Boolean portAutoIncrement,
      String portCharset,
      String portCollation,
      String portComment,
      boolean effectiveAutoIncrement,
      boolean hasDirectChange) {
  }

}
