package com.schemafy.domain.erd.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintColumnByIdPort;

import reactor.core.publisher.Mono;

@Service
public class RemoveConstraintColumnService implements RemoveConstraintColumnUseCase {

  private final DeleteConstraintColumnPort deleteConstraintColumnPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;

  public RemoveConstraintColumnService(
      DeleteConstraintColumnPort deleteConstraintColumnPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetConstraintColumnByIdPort getConstraintColumnByIdPort) {
    this.deleteConstraintColumnPort = deleteConstraintColumnPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.getConstraintColumnByIdPort = getConstraintColumnByIdPort;
  }

  @Override
  public Mono<Void> removeConstraintColumn(RemoveConstraintColumnCommand command) {
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")))
        .flatMap(constraint -> getConstraintColumnByIdPort
            .findConstraintColumnById(command.constraintColumnId())
            .switchIfEmpty(Mono.error(new RuntimeException("Constraint column not found")))
            .flatMap(constraintColumn -> {
              if (!constraintColumn.constraintId().equalsIgnoreCase(constraint.id())) {
                return Mono.error(new RuntimeException("Constraint column not found"));
              }
              return deleteConstraintColumnPort.deleteConstraintColumn(constraintColumn.id());
            }));
  }
}
