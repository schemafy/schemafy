package com.schemafy.core.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsBySchemaIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsBySchemaIdUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.SCHEMA;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = SCHEMA, id = "schemaId"))
public class GetRelationshipsBySchemaIdService implements GetRelationshipsBySchemaIdUseCase {

  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  @Override
  public Mono<List<Relationship>> getRelationshipsBySchemaId(
      GetRelationshipsBySchemaIdQuery query) {
    return getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(query.schemaId());
  }

}
