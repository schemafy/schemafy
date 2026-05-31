package com.schemafy.core.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.RELATIONSHIP;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = RELATIONSHIP, id = "relationshipId"))
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
