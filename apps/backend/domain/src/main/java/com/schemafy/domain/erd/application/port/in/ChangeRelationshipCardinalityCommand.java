package com.schemafy.domain.erd.application.port.in;

import com.schemafy.domain.erd.domain.type.Cardinality;

public record ChangeRelationshipCardinalityCommand(
    String relationshipId,
    Cardinality cardinality) {
}
