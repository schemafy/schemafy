package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteRelationshipService implements DeleteRelationshipUseCase {

  private final DeleteRelationshipPort deleteRelationshipPort;

  public DeleteRelationshipService(DeleteRelationshipPort deleteRelationshipPort) {
    this.deleteRelationshipPort = deleteRelationshipPort;
  }

  @Override
  public Mono<Void> deleteRelationship(DeleteRelationshipCommand command) {
    return deleteRelationshipPort.deleteRelationship(command.relationshipId());
  }

}
