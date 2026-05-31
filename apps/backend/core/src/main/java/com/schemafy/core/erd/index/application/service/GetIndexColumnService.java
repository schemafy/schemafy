package com.schemafy.core.erd.index.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX_COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = INDEX_COLUMN, id = "indexColumnId"))
public class GetIndexColumnService implements GetIndexColumnUseCase {

  private final GetIndexColumnByIdPort getIndexColumnByIdPort;

  @Override
  public Mono<IndexColumn> getIndexColumn(GetIndexColumnQuery query) {
    return getIndexColumnByIdPort.findIndexColumnById(query.indexColumnId())
        .switchIfEmpty(Mono.error(
            new DomainException(
                IndexErrorCode.COLUMN_NOT_FOUND,
                "Index column not found: " + query.indexColumnId())));
  }

}
