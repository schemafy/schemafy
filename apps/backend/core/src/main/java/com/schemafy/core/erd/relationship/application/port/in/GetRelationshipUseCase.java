package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipUseCase {

  Mono<Relationship> getRelationship(GetRelationshipQuery query);

}
