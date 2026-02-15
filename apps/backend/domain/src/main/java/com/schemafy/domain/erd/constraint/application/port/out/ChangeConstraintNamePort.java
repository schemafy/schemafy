package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeConstraintNamePort {

  Mono<Void> changeConstraintName(String constraintId, String newName);

}
