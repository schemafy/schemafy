package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnNamePort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

@Service
@RequiredArgsConstructor
public class ChangeColumnNameService implements ChangeColumnNameUseCase {

  private final ChangeColumnNamePort changeColumnNamePort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeColumnName(ChangeColumnNameCommand command) {
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new ColumnNotExistException("Column not found")))
        .flatMap(column -> fetchTableSchemaAndColumns(column)
            .flatMap(tuple -> applyChange(column, tuple, command.newName()))
            .thenReturn(MutationResult.<Void>of(null, column.tableId())));
  }

  private Mono<Tuple3<Table, Schema, List<Column>>> fetchTableSchemaAndColumns(Column column) {
    Mono<Table> tableMono = getTableByIdPort.findTableById(column.tableId())
        .switchIfEmpty(Mono.error(new TableNotExistException("Table not found")));

    return tableMono.flatMap(table -> {
      Mono<Schema> schemaMono = getSchemaByIdPort.findSchemaById(table.schemaId())
          .switchIfEmpty(Mono.error(new SchemaNotExistException("Schema not found")));
      Mono<List<Column>> columnsMono = getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
          .defaultIfEmpty(List.of());
      return Mono.zip(Mono.just(table), schemaMono, columnsMono);
    });
  }

  private Mono<Void> applyChange(
      Column column,
      Tuple3<Table, Schema, List<Column>> tuple,
      String newName) {
    Schema schema = tuple.getT2();
    List<Column> columns = tuple.getT3();

    String normalizedName = normalizeName(newName);
    ColumnValidator.validateName(normalizedName);
    ColumnValidator.validateReservedKeyword(schema.dbVendorName(), normalizedName);
    ColumnValidator.validateNameUniqueness(columns, normalizedName, column.id());
    return changeColumnNamePort.changeColumnName(column.id(), normalizedName);
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
