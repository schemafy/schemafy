package com.schemafy.core.erd.relationship.application.port.out;

import com.schemafy.core.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface RestoreRelationshipPort {

  Mono<Void> restoreRelationship(Relationship relationship);

}
