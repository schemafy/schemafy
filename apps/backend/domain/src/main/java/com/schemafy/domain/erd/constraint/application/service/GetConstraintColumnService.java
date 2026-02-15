package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetConstraintColumnService implements GetConstraintColumnUseCase {

  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;

  @Override
  public Mono<ConstraintColumn> getConstraintColumn(GetConstraintColumnQuery query) {
    return getConstraintColumnByIdPort.findConstraintColumnById(query.constraintColumnId())
        .switchIfEmpty(Mono.error(
            new ConstraintColumnNotExistException(
                "Constraint column not found: " + query.constraintColumnId())));
  }

}
