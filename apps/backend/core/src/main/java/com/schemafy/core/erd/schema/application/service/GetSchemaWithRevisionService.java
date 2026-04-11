package com.schemafy.core.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionResult;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetSchemaWithRevisionService implements GetSchemaWithRevisionUseCase {

  private final GetSchemaByIdPort getSchemaByIdPort;
  private final FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;

  @Override
  public Mono<GetSchemaWithRevisionResult> getSchemaWithRevision(GetSchemaQuery query) {
    return getSchemaByIdPort.findSchemaById(query.schemaId())
        .switchIfEmpty(Mono.error(
            new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + query.schemaId())))
        .flatMap(schema -> findSchemaCollaborationStatePort.findBySchemaId(query.schemaId())
            .map(state -> new GetSchemaWithRevisionResult(schema, state.currentRevision()))
            .defaultIfEmpty(new GetSchemaWithRevisionResult(schema, 0L)));
  }

}
