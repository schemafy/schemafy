package com.schemafy.domain.erd.relationship.application.port.out;

import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipKindPort {

  Mono<Void> changeRelationshipKind(String relationshipId, RelationshipKind kind);

}
