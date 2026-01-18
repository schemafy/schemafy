package com.schemafy.domain.erd.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.application.port.in.CreateSchemaResult;
import com.schemafy.domain.erd.application.port.in.ReactiveCreateSchemaUseCase;
import com.schemafy.domain.erd.application.port.out.ReactiveCreateSchema;
import com.schemafy.domain.erd.domain.Schema;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class ReactiveCreateSchemaService implements ReactiveCreateSchemaUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final ReactiveCreateSchema reactiveCreateSchema;

  @Override
  public Mono<CreateSchemaResult> createSchema(CreateSchemaCommand command) {
    return Mono.defer(() -> {
      String id = ulidGeneratorPort.generate();

      Schema schema = new Schema(
          id,
          command.projectId(),
          command.dbVendorName(),
          command.name(),
          command.charset(),
          command.collation());

      return reactiveCreateSchema.createSchema(schema)
          .map(savedSchema -> new CreateSchemaResult(
              savedSchema.id(),
              savedSchema.projectId(),
              savedSchema.dbVendorName(),
              savedSchema.name(),
              savedSchema.charset(),
              savedSchema.collation()));
    });
  }

}
