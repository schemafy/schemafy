package com.schemafy.domain.erd.relationship.application.port.out;

import com.schemafy.domain.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface CreateRelationshipPort {

  Mono<Relationship> createRelationship(Relationship relationship);

}
