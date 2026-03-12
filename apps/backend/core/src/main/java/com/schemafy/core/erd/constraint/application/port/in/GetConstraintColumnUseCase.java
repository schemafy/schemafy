package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnUseCase {

  Mono<ConstraintColumn> getConstraintColumn(GetConstraintColumnQuery query);

}
