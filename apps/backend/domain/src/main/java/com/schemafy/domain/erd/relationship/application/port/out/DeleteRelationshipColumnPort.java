package com.schemafy.domain.erd.relationship.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteRelationshipColumnPort {

  Mono<Void> deleteRelationshipColumn(String relationshipColumnId);

}
