package com.schemafy.domain.erd.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.application.port.in.CreateTableResult;
import com.schemafy.domain.erd.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.domain.Table;
import com.schemafy.domain.erd.domain.exception.TableNameDuplicateException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateTableService implements CreateTableUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateTablePort createTablePort;
  private final TableExistsPort tableExistsPort;

  @Override
  public Mono<CreateTableResult> createTable(CreateTableCommand command) {
    return tableExistsPort.existsBySchemaIdAndName(command.schemaId(), command.name())
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new TableNameDuplicateException(
                "Table name '%s' already exists in schema".formatted(command.name())));
          }

          String id = ulidGeneratorPort.generate();

          Table table = new Table(
              id,
              command.schemaId(),
              command.name(),
              command.charset(),
              command.collation());

          return createTablePort.createTable(table)
              .map(savedTable -> new CreateTableResult(
                  savedTable.id(),
                  savedTable.name(),
                  savedTable.charset(),
                  savedTable.collation()));
        });
  }

}
