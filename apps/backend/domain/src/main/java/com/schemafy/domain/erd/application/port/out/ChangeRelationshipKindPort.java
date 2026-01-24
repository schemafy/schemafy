package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipKindPort {

  Mono<Void> changeRelationshipKind(String relationshipId, RelationshipKind kind);

}
