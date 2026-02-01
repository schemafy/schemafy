package com.schemafy.domain.erd.table.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
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
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final ConstraintExistsPort constraintExistsPort;

  @Override
  public Mono<Void> changeTableName(ChangeTableNameCommand command) {
    return tableExistsPort.existsBySchemaIdAndName(command.schemaId(), command.newName())
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new TableNameDuplicateException(
                "A table with the name '" + command.newName() + "' already exists in the schema."));
          }

          return changeTableNamePort.changeTableName(command.tableId(), command.newName())
              .then(renamePkConstraint(command.schemaId(), command.tableId(), command.newName()));
        });
  }

  private Mono<Void> renamePkConstraint(String schemaId, String tableId, String newTableName) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(tableId)
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> {
          Optional<Constraint> pkOpt = constraints.stream()
              .filter(c -> c.kind() == ConstraintKind.PRIMARY_KEY)
              .findFirst();

          if (pkOpt.isEmpty()) {
            return Mono.empty();
          }

          String baseName = "pk_" + newTableName;
          return resolveUniqueConstraintName(schemaId, baseName, pkOpt.get().id())
              .flatMap(newName -> changeConstraintNamePort.changeConstraintName(
                  pkOpt.get().id(), newName));
        });
  }

  private Mono<String> resolveUniqueConstraintName(
      String schemaId, String baseName, String excludeId) {
    return resolveUniqueConstraintName(schemaId, baseName, excludeId, 0);
  }

  private Mono<String> resolveUniqueConstraintName(
      String schemaId, String baseName, String excludeId, int suffix) {
    String candidate = suffix == 0 ? baseName : baseName + "_" + suffix;
    return constraintExistsPort.existsBySchemaIdAndNameExcludingId(schemaId, candidate, excludeId)
        .flatMap(exists -> {
          if (exists) {
            return resolveUniqueConstraintName(schemaId, baseName, excludeId, suffix + 1);
          }
          return Mono.just(candidate);
        });
  }

}
