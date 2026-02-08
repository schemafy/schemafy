package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnUseCase {

  Mono<ConstraintColumn> getConstraintColumn(GetConstraintColumnQuery query);

}
