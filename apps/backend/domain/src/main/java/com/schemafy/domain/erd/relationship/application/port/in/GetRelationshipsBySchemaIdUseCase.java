package com.schemafy.domain.erd.relationship.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipsBySchemaIdUseCase {

  Mono<List<Relationship>> getRelationshipsBySchemaId(GetRelationshipsBySchemaIdQuery query);

}
