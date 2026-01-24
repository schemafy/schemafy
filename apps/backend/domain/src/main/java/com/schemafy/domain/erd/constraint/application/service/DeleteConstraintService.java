package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteConstraintService implements DeleteConstraintUseCase {

  private final DeleteConstraintPort deleteConstraintPort;
  private final DeleteConstraintColumnsByConstraintIdPort deleteConstraintColumnsPort;

  public DeleteConstraintService(
      DeleteConstraintPort deleteConstraintPort,
      DeleteConstraintColumnsByConstraintIdPort deleteConstraintColumnsPort) {
    this.deleteConstraintPort = deleteConstraintPort;
    this.deleteConstraintColumnsPort = deleteConstraintColumnsPort;
  }

  @Override
  public Mono<Void> deleteConstraint(DeleteConstraintCommand command) {
    String constraintId = command.constraintId();
    return deleteConstraintColumnsPort.deleteByConstraintId(constraintId)
        .then(deleteConstraintPort.deleteConstraint(constraintId));
  }

}
