package com.schemafy.domain.erd.relationship.application.port.out;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface CreateRelationshipColumnPort {

  Mono<RelationshipColumn> createRelationshipColumn(RelationshipColumn relationshipColumn);

}
