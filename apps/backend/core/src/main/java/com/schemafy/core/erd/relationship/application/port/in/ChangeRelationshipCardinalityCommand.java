package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.erd.relationship.domain.type.Cardinality;

public record ChangeRelationshipCardinalityCommand(
    String relationshipId,
    Cardinality cardinality) {
}
