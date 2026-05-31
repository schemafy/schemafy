package com.schemafy.core.erd.column.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = TABLE, id = "tableId"))
public class GetColumnsByTableIdService implements GetColumnsByTableIdUseCase {

  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Override
  public Mono<List<Column>> getColumnsByTableId(GetColumnsByTableIdQuery query) {
    return getColumnsByTableIdPort.findColumnsByTableId(query.tableId());
  }

}
