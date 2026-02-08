package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface ConstraintExistsPort {

  Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name);

  Mono<Boolean> existsBySchemaIdAndNameExcludingId(
      String schemaId,
      String name,
      String constraintId);

}
