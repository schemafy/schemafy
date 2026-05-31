package com.schemafy.core.erd.index.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = INDEX, id = "indexId"))
public class GetIndexColumnsByIndexIdService implements GetIndexColumnsByIndexIdUseCase {

  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Override
  public Mono<List<IndexColumn>> getIndexColumnsByIndexId(GetIndexColumnsByIndexIdQuery query) {
    return getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(query.indexId());
  }

}
