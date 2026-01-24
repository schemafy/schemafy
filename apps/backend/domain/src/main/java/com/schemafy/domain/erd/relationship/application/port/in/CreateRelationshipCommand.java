package com.schemafy.domain.erd.relationship.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

public record CreateRelationshipCommand(
    String fkTableId,
    String pkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality,
    String extra,
    List<CreateRelationshipColumnCommand> columns) {
}
