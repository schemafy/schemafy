package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface CreateRelationshipColumnPort {

  Mono<RelationshipColumn> createRelationshipColumn(RelationshipColumn relationshipColumn);

}
