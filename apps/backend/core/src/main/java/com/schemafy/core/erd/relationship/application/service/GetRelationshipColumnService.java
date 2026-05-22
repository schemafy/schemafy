package com.schemafy.core.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
public class GetRelationshipColumnService implements GetRelationshipColumnUseCase {

  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  @Override
  public Mono<RelationshipColumn> getRelationshipColumn(GetRelationshipColumnQuery query) {
    return getRelationshipColumnByIdPort
        .findRelationshipColumnById(query.relationshipColumnId())
        .switchIfEmpty(Mono.error(
            new DomainException(RelationshipErrorCode.COLUMN_NOT_FOUND,
                "Relationship column not found: " + query.relationshipColumnId())));
  }

}
