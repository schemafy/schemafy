package com.schemafy.domain.erd.column.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetColumnService implements GetColumnUseCase {

  private final GetColumnByIdPort getColumnByIdPort;

  @Override
  public Mono<Column> getColumn(GetColumnQuery query) {
    return getColumnByIdPort.findColumnById(query.columnId())
        .switchIfEmpty(Mono.error(
            new ColumnNotExistException("Column not found: " + query.columnId())));
  }

}
