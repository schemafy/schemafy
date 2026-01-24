package com.schemafy.domain.erd.column.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeColumnPositionService implements ChangeColumnPositionUseCase {

  private final ChangeColumnPositionPort changeColumnPositionPort;
  private final GetColumnByIdPort getColumnByIdPort;

  public ChangeColumnPositionService(
      ChangeColumnPositionPort changeColumnPositionPort,
      GetColumnByIdPort getColumnByIdPort) {
    this.changeColumnPositionPort = changeColumnPositionPort;
    this.getColumnByIdPort = getColumnByIdPort;
  }

  @Override
  public Mono<Void> changeColumnPosition(ChangeColumnPositionCommand command) {
    ColumnValidator.validatePosition(command.seqNo());
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new RuntimeException("Column not found")))
        .flatMap(column -> changeColumnPositionPort.changeColumnPosition(column.id(), command.seqNo()));
  }

}
