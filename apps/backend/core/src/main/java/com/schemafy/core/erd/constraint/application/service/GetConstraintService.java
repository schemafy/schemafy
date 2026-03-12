package com.schemafy.core.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintUseCase;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;

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
            new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found: " + query.constraintId())));
  }

}
