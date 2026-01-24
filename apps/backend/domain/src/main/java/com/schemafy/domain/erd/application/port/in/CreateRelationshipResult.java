package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.Cardinality;
import com.schemafy.domain.erd.domain.type.RelationshipKind;

public record CreateRelationshipResult(
    String relationshipId,
    String fkTableId,
    String pkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality,
    String extra) {
}
