package com.schemafy.domain.erd.constraint.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.constraint.domain.Constraint;

import reactor.core.publisher.Mono;

public interface GetConstraintsByTableIdUseCase {

  Mono<List<Constraint>> getConstraintsByTableId(GetConstraintsByTableIdQuery query);

}
