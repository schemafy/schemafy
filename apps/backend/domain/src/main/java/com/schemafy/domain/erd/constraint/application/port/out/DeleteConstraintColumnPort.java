package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintColumnPort {

  Mono<Void> deleteConstraintColumn(String constraintColumnId);

}
