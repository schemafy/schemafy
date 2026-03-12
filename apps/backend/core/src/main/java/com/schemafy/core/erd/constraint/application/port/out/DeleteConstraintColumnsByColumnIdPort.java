package com.schemafy.core.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintColumnsByColumnIdPort {

  Mono<Void> deleteByColumnId(String columnId);

}
