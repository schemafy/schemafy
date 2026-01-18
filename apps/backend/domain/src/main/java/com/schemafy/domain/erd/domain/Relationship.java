package com.schemafy.domain.erd.domain;

import com.schemafy.domain.erd.domain.type.Cardinality;
import com.schemafy.domain.erd.domain.type.RelationshipKind;

public record Relationship(
    String id,
    String pkTableId,
    String fkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality) {
}
