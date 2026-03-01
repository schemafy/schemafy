package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.table.application.port.in.GetTableQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetTableService implements GetTableUseCase {

  private final GetTableByIdPort getTableByIdPort;

  @Override
  public Mono<Table> getTable(GetTableQuery query) {
    return getTableByIdPort.findTableById(query.tableId())
        .switchIfEmpty(Mono.error(
            new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + query.tableId())));
  }

}
