package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteRelationshipPort {

  Mono<Void> deleteRelationship(String relationshipId);

}
