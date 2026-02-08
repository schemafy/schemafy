package com.schemafy.domain.erd.relationship.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteRelationshipColumnsByColumnIdPort {

  Mono<Void> deleteByColumnId(String columnId);

}
