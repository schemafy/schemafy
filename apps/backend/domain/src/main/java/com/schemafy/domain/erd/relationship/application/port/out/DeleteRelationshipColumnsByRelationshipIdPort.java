package com.schemafy.domain.erd.relationship.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteRelationshipColumnsByRelationshipIdPort {

  Mono<Void> deleteByRelationshipId(String relationshipId);

}
