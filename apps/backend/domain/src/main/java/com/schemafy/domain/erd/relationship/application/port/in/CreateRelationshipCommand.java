package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public record CreateRelationshipCommand(
    String fkTableId,
    String pkTableId,
    RelationshipKind kind,
    Cardinality cardinality) {
}
