package com.schemafy.domain.erd.relationship.application.port.out;

import com.schemafy.domain.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipByIdPort {

  Mono<Relationship> findRelationshipById(String relationshipId);

}
