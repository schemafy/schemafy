package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.domain.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeSchemaNameService implements ChangeSchemaNameUseCase {

  private final ChangeSchemaNamePort changeSchemaNamePort;
  private final SchemaExistsPort schemaExistsPort;
  private final GetSchemaByIdPort getSchemaByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeSchemaName(ChangeSchemaNameCommand command) {
    return getSchemaByIdPort.findSchemaById(command.schemaId())
        .switchIfEmpty(Mono.error(new SchemaNotExistException("Schema not found: " + command.schemaId())))
        .flatMap(schema -> schemaExistsPort.existsActiveByProjectIdAndName(schema.projectId(), command.newName())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new SchemaNameDuplicateException(
                    "A schema with the name '" + command.newName() + "' already exists in the project."));
              }

              return changeSchemaNamePort.changeSchemaName(command.schemaId(), command.newName())
                  .thenReturn(MutationResult.empty(null));
            }));
  }

}
