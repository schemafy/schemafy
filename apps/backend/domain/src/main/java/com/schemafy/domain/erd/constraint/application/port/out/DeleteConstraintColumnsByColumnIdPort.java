package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintColumnsByColumnIdPort {

  Mono<Void> deleteByColumnId(String columnId);

}
