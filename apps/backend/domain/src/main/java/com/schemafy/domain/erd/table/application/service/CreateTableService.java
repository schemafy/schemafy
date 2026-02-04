package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableResult;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;
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
  public Mono<MutationResult<CreateTableResult>> createTable(CreateTableCommand command) {
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
                  savedTable.collation()))
              .map(result -> MutationResult.of(result, id));
        });
  }

}
