package com.schemafy.domain.erd.relationship.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface GetRelationshipColumnsByColumnIdPort {

  Mono<List<RelationshipColumn>> findRelationshipColumnsByColumnId(String columnId);

}
