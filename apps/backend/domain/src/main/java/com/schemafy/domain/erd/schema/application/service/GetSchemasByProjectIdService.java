package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemasByProjectIdUseCase;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemasByProjectIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class GetSchemasByProjectIdService implements GetSchemasByProjectIdUseCase {

  private final GetSchemasByProjectIdPort getSchemasByProjectIdPort;

  @Override
  public Flux<Schema> getSchemasByProjectId(GetSchemasByProjectIdQuery query) {
    return getSchemasByProjectIdPort.findSchemasByProjectId(query.projectId());
  }

}
