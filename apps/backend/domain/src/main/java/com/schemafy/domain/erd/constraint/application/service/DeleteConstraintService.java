package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteConstraintService implements DeleteConstraintUseCase {

  private final DeleteConstraintPort deleteConstraintPort;

  public DeleteConstraintService(DeleteConstraintPort deleteConstraintPort) {
    this.deleteConstraintPort = deleteConstraintPort;
  }

  @Override
  public Mono<Void> deleteConstraint(DeleteConstraintCommand command) {
    return deleteConstraintPort.deleteConstraint(command.constraintId());
  }

}
