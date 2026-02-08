package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnResult;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@RequiredArgsConstructor
public class CreateColumnService implements CreateColumnUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateColumnPort createColumnPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Override
  public Mono<MutationResult<CreateColumnResult>> createColumn(CreateColumnCommand command) {
    ColumnLengthScale lengthScale = ColumnLengthScale.from(
        command.length(),
        command.precision(),
        command.scale());

    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(new TableNotExistException("Table not found")))
        .flatMap(table -> fetchSchemaAndColumns(table)
            .flatMap(tuple -> createColumn(table, tuple, command, lengthScale))
            .map(result -> MutationResult.of(result, table.id())));
  }

  private Mono<Tuple2<Schema, List<Column>>> fetchSchemaAndColumns(Table table) {
    Mono<Schema> schemaMono = getSchemaByIdPort.findSchemaById(table.schemaId())
        .switchIfEmpty(Mono.error(new SchemaNotExistException("Schema not found")));
    Mono<List<Column>> columnsMono = getColumnsByTableIdPort.findColumnsByTableId(table.id())
        .defaultIfEmpty(List.of());
    return Mono.zip(schemaMono, columnsMono);
  }

  private Mono<CreateColumnResult> createColumn(
      Table table,
      Tuple2<Schema, List<Column>> tuple,
      CreateColumnCommand command,
      ColumnLengthScale lengthScale) {
    Schema schema = tuple.getT1();
    List<Column> existingColumns = tuple.getT2();
    String normalizedName = normalizeName(command.name());
    String normalizedDataType = ColumnValidator.normalizeDataType(command.dataType());
    String charset = normalizeOptional(command.charset());
    String collation = normalizeOptional(command.collation());
    String comment = normalizeOptional(command.comment());

    ColumnValidator.validateName(normalizedName);
    ColumnValidator.validateReservedKeyword(schema.dbVendorName(), normalizedName);
    ColumnValidator.validateNameUniqueness(existingColumns, normalizedName, null);
    ColumnValidator.validateDataType(normalizedDataType);
    ColumnValidator.validateLengthScale(normalizedDataType, lengthScale);
    ColumnValidator.validateAutoIncrement(
        normalizedDataType,
        command.autoIncrement(),
        existingColumns,
        null);
    ColumnValidator.validateCharsetAndCollation(normalizedDataType, charset, collation);
    int resolvedSeqNo = resolveSeqNo(existingColumns);
    ColumnValidator.validatePosition(resolvedSeqNo);

    return Mono.fromCallable(ulidGeneratorPort::generate)
        .flatMap(id -> {
          Column column = new Column(
              id,
              table.id(),
              normalizedName,
              normalizedDataType,
              lengthScale,
              resolvedSeqNo,
              command.autoIncrement(),
              charset,
              collation,
              comment);

          return createColumnPort.createColumn(column)
              .map(savedColumn -> new CreateColumnResult(
                  savedColumn.id(),
                  savedColumn.name(),
                  savedColumn.dataType(),
                  savedColumn.lengthScale(),
                  savedColumn.seqNo(),
                  savedColumn.autoIncrement(),
                  savedColumn.charset(),
                  savedColumn.collation(),
                  savedColumn.comment()));
        });
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static int resolveSeqNo(List<Column> existingColumns) {
    return existingColumns.stream()
        .mapToInt(Column::seqNo)
        .max()
        .orElse(-1) + 1;
  }

}
