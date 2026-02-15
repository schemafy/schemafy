package com.schemafy.domain.erd.relationship.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface GetRelationshipColumnsByRelationshipIdUseCase {

  Mono<List<RelationshipColumn>> getRelationshipColumnsByRelationshipId(
      GetRelationshipColumnsByRelationshipIdQuery query);

}
