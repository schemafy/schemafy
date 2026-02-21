package com.schemafy.domain.erd.constraint.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;

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
            new DomainException(ConstraintErrorCode.COLUMN_NOT_FOUND,
                "Constraint column not found: " + query.constraintColumnId())));
  }

}
