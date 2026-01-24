package com.schemafy.domain.erd.relationship.application.port.out;

import reactor.core.publisher.Mono;

public interface CascadeDeleteRelationshipsByTableIdPort {

  Mono<Void> cascadeDeleteByTableId(String tableId);

}
