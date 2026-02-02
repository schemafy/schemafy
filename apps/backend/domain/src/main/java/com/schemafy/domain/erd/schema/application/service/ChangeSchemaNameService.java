package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.domain.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.domain.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNameDuplicateException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeSchemaNameService implements ChangeSchemaNameUseCase {

  private final ChangeSchemaNamePort changeSchemaNamePort;
  private final SchemaExistsPort schemaExistsPort;

  @Override
  public Mono<Void> changeSchemaName(ChangeSchemaNameCommand command) {
    return schemaExistsPort.existsActiveByProjectIdAndName(command.projectId(), command.newName())
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new SchemaNameDuplicateException(
                "A schema with the name '" + command.newName() + "' already exists in the project."));
          }

          return changeSchemaNamePort.changeSchemaName(command.schemaId(), command.newName());
        });
  }

}
