package com.schemafy.core.erd.relationship.application.port.out;

import com.schemafy.core.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface CreateRelationshipColumnPort {

  Mono<RelationshipColumn> createRelationshipColumn(RelationshipColumn relationshipColumn);

}
