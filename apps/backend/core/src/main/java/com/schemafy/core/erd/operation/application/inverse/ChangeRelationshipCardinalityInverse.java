package com.schemafy.core.erd.operation.application.inverse;

import com.schemafy.core.erd.relationship.domain.type.Cardinality;

public record ChangeRelationshipCardinalityInverse(
    String relationshipId,
    Cardinality oldCardinality) implements InversePayload {

}
