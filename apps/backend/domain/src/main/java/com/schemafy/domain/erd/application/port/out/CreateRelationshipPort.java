package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Relationship;

import reactor.core.publisher.Mono;

public interface CreateRelationshipPort {

  Mono<Relationship> createRelationship(Relationship relationship);

}
