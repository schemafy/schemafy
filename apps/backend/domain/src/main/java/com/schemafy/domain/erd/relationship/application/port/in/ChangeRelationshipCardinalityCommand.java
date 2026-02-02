package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.erd.relationship.domain.type.Cardinality;

public record ChangeRelationshipCardinalityCommand(
    String relationshipId,
    Cardinality cardinality) {
}
