package com.schemafy.domain.erd.column.application.service;

import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.domain.validator.ColumnValidator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeColumnPositionService implements ChangeColumnPositionUseCase {

  private final ChangeColumnPositionPort changeColumnPositionPort;
  private final GetColumnByIdPort getColumnByIdPort;

  @Override
  public Mono<Void> changeColumnPosition(ChangeColumnPositionCommand command) {
    return Mono.defer(() -> {
      ColumnValidator.validatePosition(command.seqNo());
      return getColumnByIdPort.findColumnById(command.columnId())
          .switchIfEmpty(Mono.error(new ColumnNotExistException("Column not found")))
          .flatMap(column -> changeColumnPositionPort.changeColumnPosition(column.id(), command.seqNo()));
    });
  }

}
