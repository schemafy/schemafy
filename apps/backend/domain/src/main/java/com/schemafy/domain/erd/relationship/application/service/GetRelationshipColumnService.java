package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetRelationshipColumnService implements GetRelationshipColumnUseCase {

  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  @Override
  public Mono<RelationshipColumn> getRelationshipColumn(GetRelationshipColumnQuery query) {
    return getRelationshipColumnByIdPort
        .findRelationshipColumnById(query.relationshipColumnId())
        .switchIfEmpty(Mono.error(
            new RelationshipColumnNotExistException(
                "Relationship column not found: " + query.relationshipColumnId())));
  }

}
