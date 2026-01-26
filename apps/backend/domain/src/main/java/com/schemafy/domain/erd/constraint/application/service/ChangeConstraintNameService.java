package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameDuplicateException;
import com.schemafy.domain.erd.constraint.domain.validator.ConstraintValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;

import reactor.core.publisher.Mono;

@Service
public class ChangeConstraintNameService implements ChangeConstraintNameUseCase {

  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final ConstraintExistsPort constraintExistsPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetTableByIdPort getTableByIdPort;

  public ChangeConstraintNameService(
      ChangeConstraintNamePort changeConstraintNamePort,
      ConstraintExistsPort constraintExistsPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetTableByIdPort getTableByIdPort) {
    this.changeConstraintNamePort = changeConstraintNamePort;
    this.constraintExistsPort = constraintExistsPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.getTableByIdPort = getTableByIdPort;
  }

  @Override
  public Mono<Void> changeConstraintName(ChangeConstraintNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      ConstraintValidator.validateName(normalizedName);
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")))
          .flatMap(constraint -> getTableByIdPort.findTableById(constraint.tableId())
              .switchIfEmpty(Mono.error(new RuntimeException("Table not found")))
              .flatMap(table -> constraintExistsPort.existsBySchemaIdAndNameExcludingId(
                  table.schemaId(),
                  normalizedName,
                  constraint.id())
                  .flatMap(exists -> {
                    if (exists) {
                      return Mono.error(new ConstraintNameDuplicateException(
                          "Constraint name '%s' already exists in schema".formatted(normalizedName)));
                    }
                    return changeConstraintNamePort
                        .changeConstraintName(constraint.id(), normalizedName);
                  })));
    });
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
