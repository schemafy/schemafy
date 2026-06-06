package com.schemafy.core.erd.column.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = COLUMN, id = "columnId"))
public class GetColumnService implements GetColumnUseCase {

  private final GetColumnByIdPort getColumnByIdPort;

  @Override
  public Mono<Column> getColumn(GetColumnQuery query) {
    return getColumnByIdPort.findColumnById(query.columnId())
        .switchIfEmpty(Mono.error(
            new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + query.columnId())));
  }

}
