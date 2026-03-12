package com.schemafy.core.erd.relationship.application.port.out;

import com.schemafy.core.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface CreateRelationshipPort {

  Mono<Relationship> createRelationship(Relationship relationship);

}
