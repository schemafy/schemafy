package com.schemafy.core.erd.constraint.application.port.out;

import com.schemafy.core.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnByIdPort {

  Mono<ConstraintColumn> findConstraintColumnById(String constraintColumnId);

}
