package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.type.Cardinality;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipCardinalityPort {

  Mono<Void> changeRelationshipCardinality(String relationshipId, Cardinality cardinality);

}
