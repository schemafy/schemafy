package com.schemafy.core.erd.relationship.application.port.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record CreateRelationshipCommand(
    String fkTableId,
    String pkTableId,
    RelationshipKind kind,
    Cardinality cardinality,
    JsonNode extra) {
}
