package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableResult;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.CreateTablePort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableErrorCode;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateTableService implements CreateTableUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateTablePort createTablePort;
  private final TableExistsPort tableExistsPort;
  private final GetSchemaByIdPort getSchemaByIdPort;

  @Override
  public Mono<MutationResult<CreateTableResult>> createTable(CreateTableCommand command) {
    return tableExistsPort.existsBySchemaIdAndName(command.schemaId(), command.name())
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new DomainException(TableErrorCode.NAME_DUPLICATE,
                "Table name '%s' already exists in schema".formatted(command.name())));
          }

          return getSchemaByIdPort.findSchemaById(command.schemaId())
              .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found")))
              .flatMap(schema -> Mono.fromCallable(ulidGeneratorPort::generate)
                  .flatMap(id -> {
                    String resolvedCharset = hasText(command.charset())
                        ? command.charset().trim()
                        : schema.charset();
                    String resolvedCollation = hasText(command.collation())
                        ? command.collation().trim()
                        : schema.collation();

                    Table table = new Table(
                        id,
                        command.schemaId(),
                        command.name(),
                        resolvedCharset,
                        resolvedCollation);

                    return createTablePort.createTable(table)
                        .map(savedTable -> new CreateTableResult(
                            savedTable.id(),
                            savedTable.name(),
                            savedTable.charset(),
                            savedTable.collation()))
                        .map(result -> MutationResult.of(result, id));
                  }));
        });
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

}
