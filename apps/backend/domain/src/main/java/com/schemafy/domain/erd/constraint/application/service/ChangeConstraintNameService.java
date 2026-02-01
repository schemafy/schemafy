package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.validator.ConstraintValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeConstraintNameService implements ChangeConstraintNameUseCase {

  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final ConstraintExistsPort constraintExistsPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetTableByIdPort getTableByIdPort;

  @Override
  public Mono<Void> changeConstraintName(ChangeConstraintNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      ConstraintValidator.validateName(normalizedName);
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new ConstraintNotExistException("Constraint not found")))
          .flatMap(constraint -> getTableByIdPort.findTableById(constraint.tableId())
              .switchIfEmpty(Mono.error(new TableNotExistException("Table not found")))
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
