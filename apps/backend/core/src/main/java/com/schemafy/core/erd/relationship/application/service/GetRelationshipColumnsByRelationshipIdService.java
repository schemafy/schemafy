package com.schemafy.core.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
public class GetRelationshipColumnsByRelationshipIdService
    implements GetRelationshipColumnsByRelationshipIdUseCase {

  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Override
  public Mono<List<RelationshipColumn>> getRelationshipColumnsByRelationshipId(
      GetRelationshipColumnsByRelationshipIdQuery query) {
    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(query.relationshipId());
  }

}
