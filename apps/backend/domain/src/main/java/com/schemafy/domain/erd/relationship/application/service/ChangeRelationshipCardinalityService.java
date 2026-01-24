package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipCardinalityPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;

import reactor.core.publisher.Mono;

@Service
public class ChangeRelationshipCardinalityService implements ChangeRelationshipCardinalityUseCase {

  private final ChangeRelationshipCardinalityPort changeRelationshipCardinalityPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  public ChangeRelationshipCardinalityService(
      ChangeRelationshipCardinalityPort changeRelationshipCardinalityPort,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    this.changeRelationshipCardinalityPort = changeRelationshipCardinalityPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
  }

  @Override
  public Mono<Void> changeRelationshipCardinality(ChangeRelationshipCardinalityCommand command) {
    if (command.cardinality() == null) {
      return Mono.error(new IllegalArgumentException("Relationship cardinality is required"));
    }
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> changeRelationshipCardinalityPort
            .changeRelationshipCardinality(relationship.id(), command.cardinality()));
  }

}
