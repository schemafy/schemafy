package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipColumnPositionPort {

  Mono<Void> changeRelationshipColumnPositions(
      String relationshipId,
      List<RelationshipColumn> columns);

}
