package com.schemafy.api.erd.controller.dto.response;

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
    String extra) {

  public static RelationshipResponse from(CreateRelationshipResult result) {
    return new RelationshipResponse(
        result.relationshipId(),
        result.fkTableId(),
        result.pkTableId(),
        result.name(),
        result.kind(),
        result.cardinality(),
        result.extra());
  }

  public static RelationshipResponse from(Relationship relationship) {
    return new RelationshipResponse(
        relationship.id(),
        relationship.fkTableId(),
        relationship.pkTableId(),
        relationship.name(),
        relationship.kind(),
        relationship.cardinality(),
        relationship.extra());
  }

}
