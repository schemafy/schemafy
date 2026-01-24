package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface CascadeDeleteConstraintsByTableIdPort {

  Mono<Void> cascadeDeleteByTableId(String tableId);

}
