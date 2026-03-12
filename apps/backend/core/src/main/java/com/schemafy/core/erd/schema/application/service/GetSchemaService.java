package com.schemafy.core.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;

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
            new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + query.schemaId())));
  }

}
