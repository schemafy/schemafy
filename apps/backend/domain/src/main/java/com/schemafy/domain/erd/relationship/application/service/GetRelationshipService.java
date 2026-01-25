package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetRelationshipService implements GetRelationshipUseCase {

  private final GetRelationshipByIdPort getRelationshipByIdPort;

  @Override
  public Mono<Relationship> getRelationship(GetRelationshipQuery query) {
    return getRelationshipByIdPort.findRelationshipById(query.relationshipId())
        .switchIfEmpty(Mono.error(
            new RelationshipNotExistException(
                "Relationship not found: " + query.relationshipId())));
  }

}
