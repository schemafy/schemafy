package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipErrorCode;

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
            new DomainException(RelationshipErrorCode.NOT_FOUND,
                "Relationship not found: " + query.relationshipId())));
  }

}
