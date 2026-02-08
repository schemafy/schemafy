package com.schemafy.domain.erd.relationship.domain;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public record Relationship(
    String id,
    String pkTableId,
    String fkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality,
    String extra) {
}
