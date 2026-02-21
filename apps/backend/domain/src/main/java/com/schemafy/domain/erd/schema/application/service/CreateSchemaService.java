package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.out.CreateSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateSchemaService implements CreateSchemaUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateSchemaPort createSchemaPort;
  private final SchemaExistsPort schemaExistsPort;

  @Override
  public Mono<MutationResult<CreateSchemaResult>> createSchema(CreateSchemaCommand command) {
    return schemaExistsPort
        .existsActiveByProjectIdAndName(command.projectId(), command.name())
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new DomainException(SchemaErrorCode.NAME_DUPLICATE,
                "Schema name '%s' already exists in project".formatted(command.name())));
          }

          return Mono.fromCallable(ulidGeneratorPort::generate)
              .flatMap(id -> {
                Schema schema = new Schema(
                    id,
                    command.projectId(),
                    command.dbVendorName(),
                    command.name(),
                    command.charset(),
                    command.collation());

                return createSchemaPort.createSchema(schema)
                    .map(savedSchema -> new CreateSchemaResult(
                        savedSchema.id(),
                        savedSchema.projectId(),
                        savedSchema.dbVendorName(),
                        savedSchema.name(),
                        savedSchema.charset(),
                        savedSchema.collation()))
                    .map(MutationResult::empty);
              });
        });
  }

}
