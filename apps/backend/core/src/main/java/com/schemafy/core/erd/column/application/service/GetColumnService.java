package com.schemafy.core.erd.column.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;

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
            new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + query.columnId())));
  }

}
