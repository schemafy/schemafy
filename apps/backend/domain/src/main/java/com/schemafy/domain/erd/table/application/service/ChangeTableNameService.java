package com.schemafy.domain.erd.table.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeTableNameService implements ChangeTableNameUseCase {

  private final ChangeTableNamePort changeTableNamePort;
  private final TableExistsPort tableExistsPort;

  @Override
  public Mono<Void> changeTableName(ChangeTableNameCommand command) {
    return tableExistsPort.existsBySchemaIdAndName(command.schemaId(), command.newName())
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new TableNameDuplicateException(
                "A table with the name '" + command.newName() + "' already exists in the schema."));
          }

          return changeTableNamePort.changeTableName(command.tableId(), command.newName());
        });
  }

}
