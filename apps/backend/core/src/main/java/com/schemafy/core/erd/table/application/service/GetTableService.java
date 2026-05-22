package com.schemafy.core.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
public class GetTableService implements GetTableUseCase {

  private final GetTableByIdPort getTableByIdPort;

  @Override
  public Mono<Table> getTable(GetTableQuery query) {
    return getTableByIdPort.findTableById(query.tableId())
        .switchIfEmpty(Mono.error(
            new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + query.tableId())));
  }

}
