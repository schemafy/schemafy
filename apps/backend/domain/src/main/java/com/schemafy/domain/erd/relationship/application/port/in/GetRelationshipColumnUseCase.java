package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface GetRelationshipColumnUseCase {

  Mono<RelationshipColumn> getRelationshipColumn(GetRelationshipColumnQuery query);

}
