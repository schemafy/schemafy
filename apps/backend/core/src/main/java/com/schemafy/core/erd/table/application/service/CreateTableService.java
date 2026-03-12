package com.schemafy.core.erd.table.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableResult;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.erd.table.application.port.out.CreateTablePort;
import com.schemafy.core.erd.table.application.port.out.TableExistsPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateTableService implements CreateTableUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateTablePort createTablePort;
  private final TableExistsPort tableExistsPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final TransactionalOperator transactionalOperator;

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
                        resolvedCollation,
                        command.extra());

                    return createTablePort.createTable(table)
                        .map(savedTable -> new CreateTableResult(
                            savedTable.id(),
                            savedTable.name(),
                            savedTable.charset(),
                            savedTable.collation(),
                            savedTable.extra()))
                        .map(result -> MutationResult.of(result, id));
                  }));
        })
        .as(transactionalOperator::transactional);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

}
