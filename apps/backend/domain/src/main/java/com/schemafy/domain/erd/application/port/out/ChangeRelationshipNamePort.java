package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipNamePort {

  Mono<Void> changeRelationshipName(String relationshipId, String newName);

}
