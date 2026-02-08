package com.schemafy.domain.erd.constraint.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnsByConstraintIdPort {

  Mono<List<ConstraintColumn>> findConstraintColumnsByConstraintId(String constraintId);

}
