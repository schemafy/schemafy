package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipNameUseCase {

  Mono<Void> changeRelationshipName(ChangeRelationshipNameCommand command);

}
