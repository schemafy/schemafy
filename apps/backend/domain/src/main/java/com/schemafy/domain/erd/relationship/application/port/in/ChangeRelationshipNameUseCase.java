package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipNameUseCase {

  Mono<Void> changeRelationshipName(ChangeRelationshipNameCommand command);

}
