package com.schemafy.core.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.core.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;

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
        .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + command
            .schemaId())))
        .flatMap(schema -> schemaExistsPort.existsActiveByProjectIdAndName(schema.projectId(), command.newName())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new DomainException(SchemaErrorCode.NAME_DUPLICATE,
                    "A schema with the name '" + command.newName() + "' already exists in the project."));
              }

              return changeSchemaNamePort.changeSchemaName(command.schemaId(), command.newName())
                  .thenReturn(MutationResult.empty(null));
            }));
  }

}
