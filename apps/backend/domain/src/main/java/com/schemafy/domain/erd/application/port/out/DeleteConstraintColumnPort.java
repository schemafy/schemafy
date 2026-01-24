package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintColumnPort {

  Mono<Void> deleteConstraintColumn(String constraintColumnId);

}
