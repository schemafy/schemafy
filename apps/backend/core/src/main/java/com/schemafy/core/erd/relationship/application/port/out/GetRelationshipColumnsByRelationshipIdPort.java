package com.schemafy.core.erd.relationship.application.port.out;

import java.util.List;

import com.schemafy.core.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface GetRelationshipColumnsByRelationshipIdPort {

  Mono<List<RelationshipColumn>> findRelationshipColumnsByRelationshipId(String relationshipId);

}
