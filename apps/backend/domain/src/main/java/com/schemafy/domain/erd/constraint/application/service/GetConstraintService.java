package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetConstraintService implements GetConstraintUseCase {

  private final GetConstraintByIdPort getConstraintByIdPort;

  @Override
  public Mono<Constraint> getConstraint(GetConstraintQuery query) {
    return getConstraintByIdPort.findConstraintById(query.constraintId())
        .switchIfEmpty(Mono.error(
            new ConstraintNotExistException("Constraint not found: " + query.constraintId())));
  }

}
