package com.schemafy.domain.erd.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.ChangeColumnNameCommand;
import com.schemafy.domain.erd.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.domain.erd.application.port.out.ChangeColumnNamePort;
import com.schemafy.domain.erd.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.validator.ColumnValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeColumnNameService implements ChangeColumnNameUseCase {

  private final ChangeColumnNamePort changeColumnNamePort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  public ChangeColumnNameService(
      ChangeColumnNamePort changeColumnNamePort,
      GetColumnByIdPort getColumnByIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort) {
    this.changeColumnNamePort = changeColumnNamePort;
    this.getColumnByIdPort = getColumnByIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
  }

  @Override
  public Mono<Void> changeColumnName(ChangeColumnNameCommand command) {
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new RuntimeException("Column not found")))
        .flatMap(column -> getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
            .defaultIfEmpty(List.of())
            .flatMap(columns -> applyChange(column, columns, command.newName())));
  }

  private Mono<Void> applyChange(Column column, List<Column> columns, String newName) {
    String normalizedName = normalizeName(newName);
    ColumnValidator.validateName(normalizedName);
    ColumnValidator.validateReservedKeyword(normalizedName);
    ColumnValidator.validateNameUniqueness(columns, normalizedName, column.id());
    return changeColumnNamePort.changeColumnName(column.id(), normalizedName);
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }
}
