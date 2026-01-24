package com.schemafy.domain.erd.relationship.application.port.out;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipCardinalityPort {

  Mono<Void> changeRelationshipCardinality(String relationshipId, Cardinality cardinality);

}
