package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface GetRelationshipColumnUseCase {

  Mono<RelationshipColumn> getRelationshipColumn(GetRelationshipColumnQuery query);

}
