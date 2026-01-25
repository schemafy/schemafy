package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.domain.erd.schema.application.port.in.GetSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetSchemaService implements GetSchemaUseCase {

  private final GetSchemaByIdPort getSchemaByIdPort;

  @Override
  public Mono<Schema> getSchema(GetSchemaQuery query) {
    return getSchemaByIdPort.findSchemaById(query.schemaId())
        .switchIfEmpty(Mono.error(
            new SchemaNotExistException("Schema not found: " + query.schemaId())));
  }

}
