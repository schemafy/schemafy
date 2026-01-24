package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeConstraintNameUseCase {

  Mono<Void> changeConstraintName(ChangeConstraintNameCommand command);

}
