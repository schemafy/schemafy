package com.schemafy.domain.erd;

import com.schemafy.domain.erd.type.Cardinality;
import com.schemafy.domain.erd.type.RelationshipKind;

public record Relationship(
    String id,
    String pkTableId,
    String fkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality) {
}
