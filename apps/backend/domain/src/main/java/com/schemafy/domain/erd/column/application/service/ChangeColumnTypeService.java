package com.schemafy.domain.erd.column.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeColumnTypeService implements ChangeColumnTypeUseCase {

  private final ChangeColumnTypePort changeColumnTypePort;
  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetConstraintColumnsByColumnIdPort getConstraintColumnsByColumnIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Override
  public Mono<Void> changeColumnType(ChangeColumnTypeCommand command) {
    ColumnLengthScale lengthScale = ColumnLengthScale.from(
        command.length(),
        command.precision(),
        command.scale());

    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new ColumnNotExistException("Column not found")))
        .flatMap(column -> getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
            .defaultIfEmpty(List.of())
            .flatMap(columns -> applyChange(column, columns, command.dataType(), lengthScale)));
  }

  private Mono<Void> applyChange(
      Column column,
      List<Column> columns,
      String dataType,
      ColumnLengthScale lengthScale) {
    String normalizedDataType = ColumnValidator.normalizeDataType(dataType);
    ColumnValidator.validateDataType(normalizedDataType);
    ColumnValidator.validateLengthScale(normalizedDataType, lengthScale);
    ColumnValidator.validateAutoIncrement(
        normalizedDataType,
        column.autoIncrement(),
        columns,
        column.id());
    ColumnValidator.validateCharsetAndCollation(
        normalizedDataType,
        column.charset(),
        column.collation());

    return changeColumnTypePort.changeColumnType(column.id(), normalizedDataType, lengthScale)
        .then(cascadeTypeToFkColumns(column, normalizedDataType, lengthScale, new HashSet<>()));
  }

  private Mono<Void> cascadeTypeToFkColumns(
      Column pkColumn, String dataType, ColumnLengthScale lengthScale, Set<String> visited) {
    if (!visited.add(pkColumn.id())) {
      return Mono.empty();
    }
    return getConstraintColumnsByColumnIdPort.findConstraintColumnsByColumnId(pkColumn.id())
        .defaultIfEmpty(List.of())
        .flatMap(constraintColumns -> Flux.fromIterable(constraintColumns)
            .flatMap(cc -> getConstraintByIdPort.findConstraintById(cc.constraintId()))
            .filter(constraint -> constraint.kind() == ConstraintKind.PRIMARY_KEY)
            .next()
            .flatMap(pk -> propagateTypeToFkColumns(pkColumn, dataType, lengthScale, visited)));
  }

  private Mono<Void> propagateTypeToFkColumns(
      Column pkColumn, String dataType, ColumnLengthScale lengthScale, Set<String> visited) {
    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkColumn.tableId())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .flatMap(relationship -> getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationship.id())
            .defaultIfEmpty(List.of())
            .flatMapMany(Flux::fromIterable)
            .filter(rc -> rc.pkColumnId().equals(pkColumn.id()))
            .flatMap(rc -> {
              Mono<Void> changeType = changeColumnTypePort.changeColumnType(
                  rc.fkColumnId(), dataType, lengthScale);
              Mono<Void> clearCharsetCollation = ColumnValidator.isTextType(dataType)
                  ? Mono.empty()
                  : changeColumnMetaPort.changeColumnMeta(
                      rc.fkColumnId(),
                      null,
                      "",
                      "",
                      null);

              return changeType
                  .then(clearCharsetCollation)
                  .then(getColumnByIdPort.findColumnById(rc.fkColumnId())
                      .flatMap(fkCol -> cascadeTypeToFkColumns(
                          fkCol, dataType, lengthScale, visited)));
            }))
        .then();
  }

}
