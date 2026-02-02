package com.schemafy.domain.erd.relationship.application.port.out;

import reactor.core.publisher.Mono;

public interface RelationshipExistsPort {

  Mono<Boolean> existsByFkTableIdAndName(String fkTableId, String name);

  Mono<Boolean> existsByFkTableIdAndNameExcludingId(
      String fkTableId,
      String name,
      String relationshipId);

}
