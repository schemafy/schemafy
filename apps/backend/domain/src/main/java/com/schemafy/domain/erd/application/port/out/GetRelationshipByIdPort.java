package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipByIdPort {

  Mono<Relationship> findRelationshipById(String relationshipId);

}
