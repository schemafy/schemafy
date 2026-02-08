package com.schemafy.domain.erd.relationship.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipNamePort {

  Mono<Void> changeRelationshipName(String relationshipId, String newName);

}
