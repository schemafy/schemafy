package com.schemafy.core.erd.relationship.application.port.out;

import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipKindPort {

  Mono<Void> changeRelationshipKind(String relationshipId, RelationshipKind kind);

}
