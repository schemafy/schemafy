package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnByIdPort {

  Mono<ConstraintColumn> findConstraintColumnById(String constraintColumnId);

}
