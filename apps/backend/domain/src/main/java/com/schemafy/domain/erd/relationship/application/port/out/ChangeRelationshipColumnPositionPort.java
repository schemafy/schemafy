package com.schemafy.domain.erd.relationship.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipColumnPositionPort {

  Mono<Void> changeRelationshipColumnPositions(
      String relationshipId,
      List<RelationshipColumn> columns);

}
