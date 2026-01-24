package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeConstraintNamePort {

  Mono<Void> changeConstraintName(String constraintId, String newName);

}
