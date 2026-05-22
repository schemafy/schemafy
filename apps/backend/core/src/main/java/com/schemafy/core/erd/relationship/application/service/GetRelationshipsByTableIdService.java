package com.schemafy.core.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
public class GetRelationshipsByTableIdService implements GetRelationshipsByTableIdUseCase {

  private final GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;

  @Override
  public Mono<List<Relationship>> getRelationshipsByTableId(
      GetRelationshipsByTableIdQuery query) {
    return getRelationshipsByTableIdPort.findRelationshipsByTableId(query.tableId());
  }

}
