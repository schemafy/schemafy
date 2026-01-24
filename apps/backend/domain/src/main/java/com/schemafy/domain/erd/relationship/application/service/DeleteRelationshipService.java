package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;

import reactor.core.publisher.Mono;

@Service
public class DeleteRelationshipService implements DeleteRelationshipUseCase {

  private final DeleteRelationshipPort deleteRelationshipPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort;

  public DeleteRelationshipService(
      DeleteRelationshipPort deleteRelationshipPort,
      DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort) {
    this.deleteRelationshipPort = deleteRelationshipPort;
    this.deleteRelationshipColumnsPort = deleteRelationshipColumnsPort;
  }

  @Override
  public Mono<Void> deleteRelationship(DeleteRelationshipCommand command) {
    String relationshipId = command.relationshipId();
    return deleteRelationshipColumnsPort.deleteByRelationshipId(relationshipId)
        .then(deleteRelationshipPort.deleteRelationship(relationshipId));
  }

}
