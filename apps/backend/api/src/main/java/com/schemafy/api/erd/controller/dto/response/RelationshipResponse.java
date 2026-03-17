package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record RelationshipResponse(
    String id,
    String fkTableId,
    String pkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality,
    JsonNode extra) {

  public static RelationshipResponse from(CreateRelationshipResult result,
      JsonCodec jsonCodec) {
    return new RelationshipResponse(
        result.relationshipId(),
        result.fkTableId(),
        result.pkTableId(),
        result.name(),
        result.kind(),
        result.cardinality(),
        jsonCodec.parseOptionalNode(result.extra()));
  }

  public static RelationshipResponse from(Relationship relationship,
      JsonCodec jsonCodec) {
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
