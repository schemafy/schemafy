package com.schemafy.domain.erd.relationship.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipsByTableIdPort {

  Mono<List<Relationship>> findRelationshipsByTableId(String tableId);

}
