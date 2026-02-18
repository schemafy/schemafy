package com.schemafy.domain.erd.column.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.exception.ForeignKeyColumnProtectedException;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
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

  @Override
  public Mono<MutationResult<Void>> changeColumnMeta(ChangeColumnMetaCommand command) {
    Set<String> affectedTableIds = ConcurrentHashMap.newKeySet();
    return rejectIfForeignKeyColumn(command.columnId())
        .then(getColumnByIdPort.findColumnById(command.columnId()))
        .switchIfEmpty(Mono.error(new ColumnNotExistException("Column not found")))
        .flatMap(column -> {
          affectedTableIds.add(column.tableId());
          return getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
              .defaultIfEmpty(List.of())
              .flatMap(columns -> applyChange(column, columns, command, affectedTableIds));
        })
        .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> rejectIfForeignKeyColumn(String columnId) {
    return getRelationshipColumnsByColumnIdPort.findRelationshipColumnsByColumnId(columnId)
        .flatMap(relationshipColumns -> {
          boolean isFk = relationshipColumns.stream()
              .anyMatch(rc -> rc.fkColumnId().equals(columnId));
          if (isFk) {
            return Mono.error(new ForeignKeyColumnProtectedException(
                "Foreign key column metadata cannot be changed directly"));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> applyChange(
      Column column,
      List<Column> columns,
      ChangeColumnMetaCommand command,
      Set<String> affectedTableIds) {

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
        columns,
        column.id());
    ColumnValidator.validateCharsetAndCollation(normalizedDataType, effectiveCharset, effectiveCollation);

    Boolean portAutoIncrement = command.autoIncrement().isPresent() ? effectiveAutoIncrement : null;
    String portCharset = command.charset().isPresent()
        ? Objects.toString(effectiveCharset, "")
        : null;
    String portCollation = command.collation().isPresent()
        ? Objects.toString(effectiveCollation, "")
        : null;
    String portComment = command.comment().isPresent()
        ? Objects.toString(effectiveComment, "")
        : null;

    return changeColumnMetaPort.changeColumnMeta(
        column.id(),
        portAutoIncrement,
        portCharset,
        portCollation,
        portComment)
        .then(cascadeCharsetCollationToFkColumns(
            column,
            portCharset,
            portCollation,
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

}
