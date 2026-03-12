package com.schemafy.core.erd.relationship.application.port.out;

import java.util.List;

import com.schemafy.core.erd.relationship.domain.Relationship;

import reactor.core.publisher.Mono;

public interface GetRelationshipsBySchemaIdPort {

  Mono<List<Relationship>> findRelationshipsBySchemaId(String schemaId);

}
