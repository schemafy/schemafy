package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.Constraint;

import reactor.core.publisher.Mono;

public interface GetConstraintsByTableIdPort {

  Mono<List<Constraint>> findConstraintsByTableId(String tableId);

}
