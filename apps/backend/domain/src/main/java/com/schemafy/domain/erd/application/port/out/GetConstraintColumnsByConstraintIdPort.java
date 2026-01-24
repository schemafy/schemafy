package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnsByConstraintIdPort {

  Mono<List<ConstraintColumn>> findConstraintColumnsByConstraintId(String constraintId);

}
