package com.schemafy.domain.erd.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.domain.erd.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.domain.erd.application.port.out.ChangeColumnTypePort;
import com.schemafy.domain.erd.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.ColumnLengthScale;
import com.schemafy.domain.erd.domain.validator.ColumnValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeColumnTypeService implements ChangeColumnTypeUseCase {

  private final ChangeColumnTypePort changeColumnTypePort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  public ChangeColumnTypeService(
      ChangeColumnTypePort changeColumnTypePort,
      GetColumnByIdPort getColumnByIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort) {
    this.changeColumnTypePort = changeColumnTypePort;
    this.getColumnByIdPort = getColumnByIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
  }

  @Override
  public Mono<Void> changeColumnType(ChangeColumnTypeCommand command) {
    ColumnLengthScale lengthScale = ColumnLengthScale.from(
        command.length(),
        command.precision(),
        command.scale());

    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new RuntimeException("Column not found")))
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

    return changeColumnTypePort.changeColumnType(column.id(), normalizedDataType, lengthScale);
  }
}
