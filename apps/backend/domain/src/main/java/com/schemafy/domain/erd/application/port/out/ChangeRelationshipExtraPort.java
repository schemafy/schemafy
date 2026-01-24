package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipExtraPort {

  Mono<Void> changeRelationshipExtra(String relationshipId, String extra);

}
