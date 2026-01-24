package com.schemafy.domain.erd.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.domain.type.Cardinality;
import com.schemafy.domain.erd.domain.type.RelationshipKind;

public record CreateRelationshipCommand(
    String fkTableId,
    String pkTableId,
    String name,
    RelationshipKind kind,
    Cardinality cardinality,
    String extra,
    List<CreateRelationshipColumnCommand> columns) {
}
