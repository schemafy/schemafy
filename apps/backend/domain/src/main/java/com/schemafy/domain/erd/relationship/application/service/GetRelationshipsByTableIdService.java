package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetRelationshipsByTableIdService implements GetRelationshipsByTableIdUseCase {

  private final GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;

  @Override
  public Mono<List<Relationship>> getRelationshipsByTableId(
      GetRelationshipsByTableIdQuery query) {
    return getRelationshipsByTableIdPort.findRelationshipsByTableId(query.tableId());
  }

}
