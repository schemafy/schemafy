package com.schemafy.core.erd.column.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnNamePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.domain.validator.ColumnValidator;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

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
  private final TransactionalOperator transactionalOperator;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeColumnName(ChangeColumnNameCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_COLUMN_NAME, command,
        () -> getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
        .flatMap(column -> fetchTableSchemaAndColumns(column)
            .flatMap(tuple -> applyChange(column, tuple, command.newName()))
            .thenReturn(MutationResult.<Void>of(null, column.tableId()))))
        .as(transactionalOperator::transactional);
  }

  private Mono<Tuple3<Table, Schema, List<Column>>> fetchTableSchemaAndColumns(Column column) {
    Mono<Table> tableMono = getTableByIdPort.findTableById(column.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")));

    return tableMono.flatMap(table -> {
      Mono<Schema> schemaMono = getSchemaByIdPort.findSchemaById(table.schemaId())
          .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found")));
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
