package com.schemafy.domain.erd.column.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

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
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetRelationshipColumnsByColumnIdPort getRelationshipColumnsByColumnIdPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Override
  public Mono<Void> changeColumnMeta(ChangeColumnMetaCommand command) {
    return rejectIfForeignKeyColumn(command.columnId())
        .then(getColumnByIdPort.findColumnById(command.columnId()))
        .switchIfEmpty(Mono.error(new ColumnNotExistException("Column not found")))
        .flatMap(column -> getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
            .defaultIfEmpty(List.of())
            .flatMap(columns -> applyChange(column, columns, command)));
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
      ChangeColumnMetaCommand command) {
    boolean nextAutoIncrement = command.autoIncrement() != null
        ? command.autoIncrement()
        : column.autoIncrement();
    String nextCharset = hasText(command.charset()) ? command.charset() : column.charset();
    String nextCollation = hasText(command.collation()) ? command.collation() : column.collation();
    String nextComment = hasText(command.comment()) ? command.comment() : column.comment();

    String normalizedDataType = ColumnValidator.normalizeDataType(column.dataType());
    ColumnValidator.validateAutoIncrement(
        normalizedDataType,
        nextAutoIncrement,
        columns,
        column.id());
    ColumnValidator.validateCharsetAndCollation(normalizedDataType, nextCharset, nextCollation);

    String normalizedCharset = normalizeOptional(nextCharset);
    String normalizedCollation = normalizeOptional(nextCollation);

    return changeColumnMetaPort.changeColumnMeta(
        column.id(),
        nextAutoIncrement,
        normalizedCharset,
        normalizedCollation,
        normalizeOptional(nextComment))
        .then(cascadeCharsetCollationToFkColumns(
            column, normalizedCharset, normalizedCollation, new HashSet<>()));
  }

  private Mono<Void> cascadeCharsetCollationToFkColumns(
      Column column, String charset, String collation, Set<String> visited) {
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
                column, charset, collation, visited)));
  }

  private Mono<Void> propagateCharsetCollationToFkColumns(
      Column pkColumn, String charset, String collation, Set<String> visited) {
    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .flatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationship.id())
            .defaultIfEmpty(List.of())
            .flatMapMany(Flux::fromIterable)
            .filter(rc -> rc.pkColumnId().equals(pkColumn.id()))
            .flatMap(rc -> changeColumnMetaPort.changeColumnMeta(
                rc.fkColumnId(), null, charset, collation, null)
                .then(getColumnByIdPort.findColumnById(rc.fkColumnId())
                    .flatMap(fkCol -> cascadeCharsetCollationToFkColumns(
                        fkCol, charset, collation, visited)))))
        .then();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

}
