package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeColumnMetaService implements ChangeColumnMetaUseCase {

  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  public ChangeColumnMetaService(
      ChangeColumnMetaPort changeColumnMetaPort,
      GetColumnByIdPort getColumnByIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort) {
    this.changeColumnMetaPort = changeColumnMetaPort;
    this.getColumnByIdPort = getColumnByIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
  }

  @Override
  public Mono<Void> changeColumnMeta(ChangeColumnMetaCommand command) {
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new RuntimeException("Column not found")))
        .flatMap(column -> getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
            .defaultIfEmpty(List.of())
            .flatMap(columns -> applyChange(column, columns, command)));
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

    return changeColumnMetaPort.changeColumnMeta(
        column.id(),
        nextAutoIncrement,
        normalizeOptional(nextCharset),
        normalizeOptional(nextCollation),
        normalizeOptional(nextComment));
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
