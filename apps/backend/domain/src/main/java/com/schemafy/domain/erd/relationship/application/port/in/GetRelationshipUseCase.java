package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipUseCase {

  Mono<Relationship> getRelationship(GetRelationshipQuery query);

}
