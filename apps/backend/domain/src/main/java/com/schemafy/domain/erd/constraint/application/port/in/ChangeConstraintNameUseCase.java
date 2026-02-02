package com.schemafy.domain.erd.constraint.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeConstraintNameUseCase {

  Mono<Void> changeConstraintName(ChangeConstraintNameCommand command);

}
