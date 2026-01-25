package com.schemafy.domain.erd.constraint.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

public interface GetConstraintColumnsByConstraintIdUseCase {

  Mono<List<ConstraintColumn>> getConstraintColumnsByConstraintId(
      GetConstraintColumnsByConstraintIdQuery query);

}
