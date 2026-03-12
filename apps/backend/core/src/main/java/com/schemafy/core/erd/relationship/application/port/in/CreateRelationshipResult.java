package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record CreateRelationshipResult(
    String relationshipId,
    String fkTableId,
    String pkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality,
    String extra) {
}
