package com.schemafy.core.erd.index.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
public class GetIndexesByTableIdService implements GetIndexesByTableIdUseCase {

  private final GetIndexesByTableIdPort getIndexesByTableIdPort;

  @Override
  public Mono<List<Index>> getIndexesByTableId(GetIndexesByTableIdQuery query) {
    return getIndexesByTableIdPort.findIndexesByTableId(query.tableId());
  }

}
