package com.schemafy.api.erd.service.relationship;

import org.springframework.stereotype.Component;

import com.schemafy.api.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.core.erd.relationship.domain.Relationship;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RelationshipApiResponseMapper {

  private final JsonCodec jsonCodec;

  public RelationshipResponse toRelationshipResponse(
      CreateRelationshipResult result) {
    return new RelationshipResponse(
        result.relationshipId(),
        result.fkTableId(),
        result.pkTableId(),
        result.name(),
        result.kind(),
        result.cardinality(),
        jsonCodec.parseOptionalNode(result.extra()));
  }

  public RelationshipResponse toRelationshipResponse(
      Relationship relationship) {
    return new RelationshipResponse(
        relationship.id(),
        relationship.fkTableId(),
        relationship.pkTableId(),
        relationship.name(),
        relationship.kind(),
        relationship.cardinality(),
        jsonCodec.parseOptionalNode(relationship.extra()));
  }

}
