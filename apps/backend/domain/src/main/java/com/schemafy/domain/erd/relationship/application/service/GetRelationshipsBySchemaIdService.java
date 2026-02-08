package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsBySchemaIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsBySchemaIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetRelationshipsBySchemaIdService implements GetRelationshipsBySchemaIdUseCase {

  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  @Override
  public Mono<List<Relationship>> getRelationshipsBySchemaId(
      GetRelationshipsBySchemaIdQuery query) {
    return getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(query.schemaId());
  }

}
