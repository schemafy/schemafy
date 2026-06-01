package com.schemafy.core.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.application.port.out.GetTablesBySchemaIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.SCHEMA;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = SCHEMA, id = "schemaId"))
public class GetTablesBySchemaIdService implements GetTablesBySchemaIdUseCase {

  private final GetTablesBySchemaIdPort getTablesBySchemaIdPort;

  @Override
  public Flux<Table> getTablesBySchemaId(GetTablesBySchemaIdQuery query) {
    return getTablesBySchemaIdPort.findTablesBySchemaId(query.schemaId());
  }

}
